SUMMARY = "SCP Firmware"
DESCRIPTION = "SCP Firmware"
HOMEPAGE = "https://gitlab.arm.com/firmware/SCP-firmware"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://license.md;md5=ef610a65bfb6d16f79778877cbfd45df"

COMPATIBLE_MACHINE = "ironhide"
PACKAGE_ARCH = "${MACHINE_ARCH}"

DEPENDS = " \
    gcc-arm-none-eabi-native \
    cmake-native \
    ninja-native \
"

inherit deploy

PV:rcar-gen5:ironhide = "v2.16.0+upstream+git${SRCPV}"
BRANCH:rcar-gen5:ironhide = "renesas"
SRC_URI = "gitsm://gitlab.arm.com/clegoffic/SCP-firmware.git;branch=${BRANCH};protocol=https"
SRCREV:rcar-gen5:ironhide = "4692513a71840034c62fd013ef75f630fd49e187"
TARGET_PRODUCT:rcar-gen5:ironhide = "rcar5"

S = "${WORKDIR}/git"

# do_install() nothing
do_install[noexec] = "1"
# do_configure() nothing to do
do_install[configure] = "1"

OUTPUT_PATH = "${S}/build/rcar5/GNU/release/firmware-scp_ramfw/bin"
do_compile () {
    oe_runmake -f Makefile.cmake PRODUCT=${TARGET_PRODUCT}
    ELF_PATH=${OUTPUT_PATH}/rcar5-bl2.elf
    SREC_PATH=${OUTPUT_PATH}/rcar5-bl2.srec
    arm-none-eabi-objcopy -O srec --srec-forceS3 ${ELF_PATH} ${SREC_PATH}
}

do_deploy () {
    # Copy binary files to deploy directory
    install -m 0644 ${OUTPUT_PATH}/*.elf  ${DEPLOYDIR}
    install -m 0644 ${OUTPUT_PATH}/*.bin  ${DEPLOYDIR}
    install -m 0644 ${OUTPUT_PATH}/*.srec ${DEPLOYDIR}
}

addtask deploy after do_compile

