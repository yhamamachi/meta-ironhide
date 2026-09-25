DESCRIPTION = "OP-TEE OS"

LICENSE = "BSD-2-Clause & BSD-3-Clause"
LIC_FILES_CHKSUM = " \
    file://LICENSE;md5=c1f21c4f72f372ef38a5a4aee55ec173 \
"

PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit deploy python3native

PV = "4.4+renesas+git${SRCPV}"

BRANCH = "rcar-gen5_4.4.0"
SRCREV = "03f315805131bed4d4282045464e04b504797099"

SRC_URI = " \
    git://github.com/renesas-rcar/optee_os.git;branch=${BRANCH};protocol=https \
"
S = "${WORKDIR}/git"

COMPATIBLE_MACHINE = "ironhide"
PLATFORM = "rcar_gen5"

DEPENDS = "python3-pycryptodome-native python3-pyelftools-native python3-cryptography-native libgcc"

export CROSS_COMPILE64="${TARGET_PREFIX}"
CFLAGS += "--sysroot=${RECIPE_SYSROOT}"

# Let the Makefile handle setting up the flags as it is a standalone application
#LD[unexport] = "1"
LDFLAGS[unexport] = "1"
libdir[unexport] = "1"

EXTRA_OEMAKE = "-e MAKEFLAGS= PLATFORM=${PLATFORM} CFG_ARM64_core=y CFG_INSECURE=y RCAR_DEBUG_LOG=0 CFG_SCIF=n"

do_compile() {
    export CRYPTOGRAPHY_OPENSSL_NO_LEGACY=1
    oe_runmake
}

# do_install() nothing
do_install[noexec] = "1"

do_deploy() {
    # Create deploy folder
    install -d ${DEPLOYDIR}

    # Copy TEE OS to deploy folder
    install -m 0644 ${S}/out/arm-plat-${PLATFORM}/core/tee.elf ${DEPLOYDIR}/apu-tee-${MACHINE}.elf
    install -m 0644 ${S}/out/arm-plat-${PLATFORM}/core/tee.bin ${DEPLOYDIR}/apu-tee-${MACHINE}.bin
    install -m 0644 ${S}/out/arm-plat-${PLATFORM}/core/tee.srec ${DEPLOYDIR}/apu-tee-${MACHINE}.srec
}

addtask deploy before do_build after do_compile

