DESCRIPTION = "OP-TEE Client"
LICENSE = "BSD-2-Clause"

LIC_FILES_CHKSUM = "file://LICENSE;md5=69663ab153298557a59c67a60a743e5b"
PR = "r0"
PV = "4.4.0+renesas+git${SRCPV}"
BRANCH = "master"
SRC_URI = "git://github.com/OP-TEE/optee_client.git;branch=${BRANCH};protocol=https"
SRCREV = "d221676a58b305bddbf97db00395205b3038de8e"

DEPENDS += "pkgconfig-native util-linux"

SRC_URI += " \
    file://optee.service \
    file://0001-tee-supplicant-Delete-the-sleep-time-when-writing-da.patch \
"

inherit python3native systemd
SYSTEMD_SERVICE:${PN} = "optee.service"

COMPATIBLE_MACHINE = "ironhide"

PACKAGE_ARCH = "${MACHINE_ARCH}"

S = "${WORKDIR}/git"

EXTRA_OEMAKE = "PKG_CONFIG=pkg-config  RPMB_EMU=0"

do_install () {
    # Create destination directories
    install -d ${D}/${libdir}
    install -d ${D}/${bindir}
    install -d ${D}/${includedir}

    # Install library
    install -m 0755 ${S}/out/export/usr/lib/libteec.so.2.0.0 ${D}/${libdir}
    # Create symbolic link
    cd ${D}/${libdir}
    ln -sf libteec.so.2.0.0 libteec.so.2.0
    ln -sf libteec.so.2.0 libteec.so.2
    ln -sf libteec.so.2 libteec.so

    # Install header files
    install -m 0644 ${S}/out/export/usr/include/* ${D}/${includedir}

    # Install binary to bindir
    install -m 0755 ${S}/out/export/usr/sbin/tee-supplicant ${D}/${bindir}

    # Install systemd service configure file for OP-TEE client
    if ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'true', 'false', d)}; then
        install -d ${D}/${systemd_system_unitdir}
        install -m 0644 ${WORKDIR}/optee.service ${D}/${systemd_system_unitdir}
    fi
}

RPROVIDES:${PN} += "optee-client"

