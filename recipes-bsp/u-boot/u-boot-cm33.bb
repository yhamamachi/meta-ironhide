FILESEXTRAPATHS:prepend := "${THISDIR}/${BPN}:"

require recipes-bsp/u-boot/u-boot-common.inc
require recipes-bsp/u-boot/u-boot.inc
PROVIDES = "u-boot-cm33"

COMPATIBLE_MACHINE = "(ironhide)"

DEPENDS += "lzop-native srecord-native"
DEPENDS += "bc-native dtc-native python3-pyelftools-native gnutls-native"
DEPENDS += " \
    gcc-arm-none-eabi-native \
"
EXTRA_OEMAKE+=" ARCH=arm CROSS_COMPILE=arm-none-eabi- CC=arm-none-eabi-gcc"

UBOOT_URL = "git://github.com/u-boot/u-boot.git"
BRANCH = "main"
SRCREV = "a06e89ab05eaa2b8344d521319399333cd760ae5"
LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=2ca5f2c35c8cc335f0a19756634782f1"

SRC_URI = "${UBOOT_URL};branch=${BRANCH};protocol=https"

PV = "v2026.10-rc5+git${SRCPV}"

UBOOT_SREC_SUFFIX = "srec"
UBOOT_SREC ?= "u-boot-elf.${UBOOT_SREC_SUFFIX}"
UBOOT_SREC_CM33 ?= "u-boot-elf-cm33.${UBOOT_SREC_SUFFIX}"
UBOOT_SHDR ?= "u-boot-elf.shdr"
UBOOT_SREC_IMAGE ?= "u-boot-elf-cm33-${MACHINE}-${PV}-${PR}.${UBOOT_SREC_SUFFIX}"
UBOOT_SREC_IMAGE_FOR_SCIF_DL_MODE ?= "u-boot-elf-cm33-${MACHINE}-scif-download.${UBOOT_SREC_SUFFIX}"
UBOOT_SREC_SYMLINK ?= "u-boot-elf-cm33-${MACHINE}.${UBOOT_SREC_SUFFIX}"
UBOOT_MACHINE = "r8a78000_ironhide_cm33_defconfig"

# do_install() nothing
do_install[noexec] = "1"
# do_configure() nothing to do
do_install[configure] = "1"

# Override deploy function
do_deploy() {
    if [ -n "${UBOOT_CONFIG}" ]
    then
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                if [ $j -eq $i ]
                then
                    type=${type#*_}
                    install -m 644 ${B}/${config}/${UBOOT_SREC} ${DEPLOYDIR}/u-boot-cm33-elf-${type}-${PV}-${PR}.${UBOOT_SREC_SUFFIX}
                    cd ${DEPLOYDIR}
                    ln -sf u-boot-cm33-elf-${type}-${PV}-${PR}.${UBOOT_SREC_SUFFIX} u-boot-cm33-elf-${type}.${UBOOT_SREC_SUFFIX}
                fi
            done
            unset j
        done
        unset i
    else
        install -m 644 ${B}/${UBOOT_SREC} ${DEPLOYDIR}/${UBOOT_SREC_IMAGE}
        install -m 644 ${B}/${config}/${UBOOT_SHDR} ${DEPLOYDIR}/u-boot-elf-cm33-${MACHINE}.shdr
        cd ${DEPLOYDIR}
        rm -f ${UBOOT_SREC} ${UBOOT_SREC_SYMLINK} ${UBOOT_SREC_IMAGE_FOR_SCIF_DL_MODE}
        ln -sf ${UBOOT_SREC_IMAGE} ${UBOOT_SREC_SYMLINK}
        ln -sf ${UBOOT_SREC_IMAGE} ${UBOOT_SREC_CM33}

        # Generate U-Boot for SCIF download mode
        cp -f ${UBOOT_SREC_IMAGE} ${UBOOT_SREC_IMAGE_FOR_SCIF_DL_MODE}
        sed -i ${UBOOT_SREC_IMAGE_FOR_SCIF_DL_MODE} \
            -e "1a S30D1840201000004118000006000B"
    fi
}

