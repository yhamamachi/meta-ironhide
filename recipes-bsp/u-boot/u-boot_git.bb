FILESEXTRAPATHS:prepend := "${THISDIR}/${BPN}:"

require recipes-bsp/u-boot/u-boot-common.inc
require recipes-bsp/u-boot/u-boot.inc

COMPATIBLE_MACHINE = "(ironhide)"

DEPENDS += "lzop-native srecord-native"
DEPENDS += "bc-native dtc-native python3-pyelftools-native gnutls-native"

UBOOT_URL = "git://github.com/u-boot/u-boot.git"
BRANCH = "main"
SRCREV = "a06e89ab05eaa2b8344d521319399333cd760ae5"
LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=2ca5f2c35c8cc335f0a19756634782f1"

SRC_URI = "${UBOOT_URL};branch=${BRANCH};protocol=https"

PV = "v2026.10-rc5+git${SRCPV}"

UBOOT_SREC_SUFFIX = "srec"
UBOOT_SREC ?= "u-boot-elf.${UBOOT_SREC_SUFFIX}"
UBOOT_SREC_IMAGE ?= "u-boot-elf-${MACHINE}-${PV}-${PR}.${UBOOT_SREC_SUFFIX}"
UBOOT_SREC_SYMLINK ?= "u-boot-elf-${MACHINE}.${UBOOT_SREC_SUFFIX}"

SRC_URI:append = "\
    file://nfs_cmd.cfg \
    file://mmc_boot.cfg \
    file://preboot.cfg \
"
SRC_URI:append = "\
    file://scmi/0001-arm-dts-renesas-Drop-unreachable-high-DRAM-banks-on-.patch \
    file://scmi/0002-firmware-scmi-Poll-the-shared-buffer-for-the-respons.patch \
    file://scmi/0003-clk-renesas-Accept-the-SCP-firmware-SCMI-version.patch \
    file://scmi/0004-power-domain-renesas-r8a78000-Accept-the-SCP-firmwar.patch \
    file://scmi/0005-mailbox-renesas-Receive-and-clear-the-MFIS-doorbell.patch \
"

do_deploy:append() {
    if [ -n "${UBOOT_CONFIG}" ]
    then
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                if [ $j -eq $i ]
                then
                    type=${type#*_}
                    install -m 644 ${B}/${config}/${UBOOT_SREC} ${DEPLOYDIR}/u-boot-elf-${type}-${PV}-${PR}.${UBOOT_SREC_SUFFIX}
                    cd ${DEPLOYDIR}
                    ln -sf u-boot-elf-${type}-${PV}-${PR}.${UBOOT_SREC_SUFFIX} u-boot-elf-${type}.${UBOOT_SREC_SUFFIX}
                fi
            done
            unset j
        done
        unset i
    else
        install -m 644 ${B}/${UBOOT_SREC} ${DEPLOYDIR}/${UBOOT_SREC_IMAGE}
        cd ${DEPLOYDIR}
        rm -f ${UBOOT_SREC} ${UBOOT_SREC_SYMLINK}
        ln -sf ${UBOOT_SREC_IMAGE} ${UBOOT_SREC_SYMLINK}
        ln -sf ${UBOOT_SREC_IMAGE} ${UBOOT_SREC}
    fi
}

