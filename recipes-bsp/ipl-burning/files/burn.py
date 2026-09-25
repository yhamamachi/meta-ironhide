#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
# Copyright (c) 2025-2026, Renesas Electronics Corporation. All rights reserved.
# SPDX-License-Identifier: MIT
#

# import section
import os
import sys
import shutil
import subprocess
import serial.tools.list_ports # pyserial
import colorama
from tqdm import tqdm
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# Global variables
WORK_DIR = os.path.dirname(os.path.abspath(__file__))
IPL_PATH=f"{WORK_DIR}"
MOT_PATH=f"{WORK_DIR}"
CM33_SCIF_DOWNLOAD_BINARY=f"{WORK_DIR}/u-boot-elf-cm33-ironhide-scif-download.srec"
CM33_UBOOT_PATH=f"{WORK_DIR}/u-boot-elf-cm33-ironhide.srec"
CM33_HEADER_PATH=f"{WORK_DIR}/u-boot-elf-cm33-ironhide.shdr"

# For serial port detection
TARGET_MESSAGE = "please send !"
TIMEOUT_SECONDS = 30 # Port scanning timeout
BAUDRATE = 1843200

# Functions
def print_err(str):
    colorama.init()
    print(f'{colorama.Fore.RED}{str}{colorama.Style.RESET_ALL}')

def Usage():
    print(f"Usage:")
    print(f"    {sys.argv[0]} board <comport or serial_device>")
    print(f"comport or serial_device:")
    for comport in serial.tools.list_ports.comports():
        print(f"    {comport}")

import serial
import threading
import time
def monitor_port(port_info, result, target_message=TARGET_MESSAGE):
    port = port_info.device
    try:
        with serial.Serial(port, baudrate=BAUDRATE, timeout=1) as ser:
            start_time = time.time()
            while (result["port"] is None) and (time.time() - start_time < TIMEOUT_SECONDS):
                if ser.in_waiting:
                    line = ser.readline().decode(errors='ignore').strip()
                    # print(f"DEBUG: [{port}] {line}")
                    if target_message in line:
                        result["port"] = port
                        return
    except:
        # print(f"[{port}] DEBUG: Thread is closed by Error")
        pass
    # print("DEBUG: Thread is closed by Timeout")

def detect_serial_device(target_message=TARGET_MESSAGE):
    _ports = list(serial.tools.list_ports.comports())
    ports = [port for port in _ports if not os.path.islink(port.device)] # list up except symbolic link
    if len(ports) == 0:
        print("No serial ports found.")
        return None

    print(f"Scanning {len(ports)} serial ports for message: '{target_message}'")
    threads = []
    result = {"port": None}

    for port_info in ports:
        t = threading.Thread(target=monitor_port, args=(port_info, result, target_message))
        t.start()
        threads.append(t)

    for t in threads:
        t.join()
        if result["port"] is not None:
            return result["port"]

def print_chapter(msg):
    msg_len = len(msg)
    print( "****************************************") # 40 char
    print(f"* {msg}",end="")
    for i in range(40-msg_len-4):
        print(" ", end="")
    print(" *")
    print( "****************************************");print("")


def print_dip_sw(status=0x55):
    sw = [0]*8
    print("")
    # ascii art
    print("  |1|2|3|4|5|6|7|8|")
    print("  |-|-|-|-|-|-|-|-|")
    for i in range(8): sw[7-i] = "x" if (status>>i) & 0x01 else " "
    print(f"  |{sw[0]}|{sw[1]}|{sw[2]}|{sw[3]}|{sw[4]}|{sw[5]}|{sw[6]}|{sw[7]}| ON")
    for i in range(8): sw[7-i] = " " if (status>>i) & 0x01 else "x"
    print(f"  |{sw[0]}|{sw[1]}|{sw[2]}|{sw[3]}|{sw[4]}|{sw[5]}|{sw[6]}|{sw[7]}| OFF")
    print("")
    # table
    for i in range(8): sw[7-i] = "ON " if (status>>i) & 0x01 else "OFF"
    print( "  | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |")
    print( "  |---|---|---|---|---|---|---|---|")
    print(f"  |{sw[0]}|{sw[1]}|{sw[2]}|{sw[3]}|{sw[4]}|{sw[5]}|{sw[6]}|{sw[7]}|")
    print("")

# SW43 Default value 00100100
def ironhide_instruction_setup():
    print_chapter("ipl burting tool for ironhie")
    print_chapter("1. Board setup for flashing firmware")
    print("Power off the board, then please set DIP-SW(SW43) as following")
    print_dip_sw(status=0xa4) # 1| 1010 0100 | 8
    print("Then, connect board to PC using USB cable")
    print("  If serial port has already been opened by other application(*),")
    print("  please close the application to use serial port by this script")
    print("  *) Teraterm, picocom, and so on.")
    print("")
    print("Press Enter key to proceed: "); input()
    print("")
    print_chapter("2. Start flashing firmware")
    print("Please power on the board"); print("")

def ironhide_instruction_tidyup():
    print_chapter("3. Setup board for booting OS image")
    print("Power off the board, then please set DIP-SW(SW43) as following")
    print_dip_sw(status=0x04) # 1| 0000 0100 | 8
    print("Now, you can use bootloader on your board to boot OS image")
    print("")
    print("Close this window(script) or press Enter key to close program: "); input()

def serial_wait(ser, target_message, timeout_seconds=TIMEOUT_SECONDS):
    start_time = time.time()
    while ((time.time() - start_time) < timeout_seconds):
        if ser.in_waiting:
            line = ser.readline().decode(errors='ignore').strip()
            #print(line)
            if target_message in line:
                return True
            elif "." in line:
                # Reset timeout timer
                start_time = time.time()
    return False

def serial_send_file_with_progress(ser, filepath, delay=0.001):
    with open(filepath, "rb") as f:
        with tqdm(total=os.fstat(f.fileno()).st_size,
                unit="B",
                unit_scale=True,
                desc="Sending S-record",
            ) as pbar:
            for line in f:
                data = line.rstrip(b"\r\n") + b"\r\n"
                ser.write(data)
                ser.flush()
                if delay != 0:
                    time.sleep(delay)
                pbar.update(len(line))
    print("")

def ironhide_flash_uboot_cm33(main_port):
    hyperflash_update_command_list = [
        "protect off ${flash_addr} +${filesize}",
        "erase ${flash_addr} +${filesize}",
        "cp.b ${loadaddr} ${flash_addr} ${filesize}",
        "cmp.b ${loadaddr} ${flash_addr} ${filesize}",
    ]

    # Send U-Boot CM33
    with serial.Serial(main_port, baudrate=BAUDRATE, timeout=1) as ser:
        # Bootrom handle highspeed transfer, so delay is disabled.
        serial_send_file_with_progress(ser, CM33_SCIF_DOWNLOAD_BINARY, delay=0)

    # Detect U-Boot CM33 port, then flash U-Boot
    sub_port=detect_serial_device(target_message="U-Boot")
    if sub_port is None:
        print_err(f"ERROR: U-boot on CM33 is not detected")
        print("")
        print("Close this window or press Enter key: "); input()
        quit()
    else:
        print(f"Port {sub_port} is detected for U-Boot CM33")

    with serial.Serial(sub_port, baudrate=BAUDRATE, timeout=1) as ser:
        if serial_wait(ser, "Hit any key to stop autoboot"):
            ser.write("\r".encode())
            time.sleep(1)
        if serial_wait(ser, "=>"):
            ser.write("setenv loadaddr 0x60000000\r".encode())

        # DRAM top: 0x6000_0000, Hyperflash top: 0x3400_0000, srec_top: 0x1840_0000
        # Load U-Boot header
        print("==== Loading U-Boot header ====")
        srec_load_offset = hex(0x6000_0000 - 0x1840_0000)
        ser.write(f"loads {srec_load_offset}\r".encode())
        if serial_wait(ser, "=>"):
            serial_send_file_with_progress(ser, CM33_HEADER_PATH)
            time.sleep(1)
        # Write U-Boot header to Hyperflash(0x34000000)
        if serial_wait(ser, "=>"):
            ser.write("setenv flash_addr 0x34000000\r".encode())
        print("Update hyperflash ... ", end="", flush=True)
        for cmd in hyperflash_update_command_list:
            if serial_wait(ser, "=>"):
                _cmd = cmd + "\r"
                ser.write(_cmd.encode())
        if serial_wait(ser, " were the same", timeout_seconds=120) is False:
            print("Write image is not same as image on DRAM.")
            print("Please retry the flashing procedure.")
            quit()
        print("Done")

        # Load U-Boot binary
        print("==== Loading U-Boot ====")
        srec_load_offset = hex(0x6000_0000 - 0x1841_0000)
        ser.write(f"loads {srec_load_offset}\r".encode())
        if serial_wait(ser, "=>"):
            serial_send_file_with_progress(ser, CM33_UBOOT_PATH)
            time.sleep(1)
        # Write U-Boot to Hyperflash(0x34040000)
        if serial_wait(ser, "=>"):
            ser.write("setenv flash_addr 0x34040000\r".encode())
        print("Update hyperflash ... ", end="", flush=True)
        for cmd in hyperflash_update_command_list:
            if serial_wait(ser, "=>"):
                _cmd = cmd + "\r"
                ser.write(_cmd.encode())
        if serial_wait(ser, " were the same") is False:
            print("Write image is not same as image on DRAM.")
            print("Please retry the flashing procedure.")
            quit()
        print("Done")

# main function
def main():
    COM_PORT = "/dev/ttyUSBXX or COMXX"
    BURN_MODE = "all"
    INSTRUCTION_MODE = False

    args = sys.argv
    if "-h" in args:
        Usage(); quit()

    # Instruction mode
    if len(args) <= 1:
        INSTRUCTION_MODE = True
        ironhide_instruction_setup()
        COM_PORT=detect_serial_device()
        if COM_PORT is None:
            print_err(f"ERROR: Target board is not found.")
            print("")
            print("Close this window or press Enter key: "); input()
            quit()
        else:
            print(f"Port {COM_PORT} is detected")
    # Automatic mode
    elif args[1] not in [comport.device for comport in serial.tools.list_ports.comports()]:
        print_err(f"ERROR: Please \"input\" correct comport:")
        Usage(); quit()
    else:
        COM_PORT=args[1]

    ironhide_flash_uboot_cm33(main_port=COM_PORT)

    if INSTRUCTION_MODE is True:
        ironhide_instruction_tidyup()

if __name__ == "__main__":
    main()

