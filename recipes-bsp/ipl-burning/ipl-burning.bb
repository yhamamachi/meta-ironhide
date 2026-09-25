DESCRIPTION = "IPL burning tool"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${WORKDIR}/LICENSE.MIT;md5=e7d4fc574e1858d0f946f9aa32397c5a"

PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit deploy
DEPENDS:append = "unzip-native python3-native"
do_compile[depends] += "u-boot:do_deploy"

COMPATIBLE_MACHINE = "ironhide"

ALLOW_EMPTY:${PN} = "1"
ALLOW_EMPTY:${PN}-dev = "1"
ALLOW_EMPTY:${PN}-staticdev = "1"

SRC_URI:append = " \
    file://burn.py \
    file://run.bat \
    file://run.sh \
    file://LICENSE-index.txt \
    file://LICENSE.MIT \
"

# do_configure() nothing
do_configure[noexec] = "1"
# do_compile() nothing
do_compile[noexec] = "1"
# do_install() nothing
do_install[noexec] = "1"

# Wait for U-Boot
do_deploy[depends] += "u-boot-cm33:do_deploy"

# Wait for U-Boot licenase generation
do_deploy[depends] += "u-boot-cm33:do_populate_lic"
MACHINE_LIC = "${@d.getVar('MACHINE').replace('-', '_')}"

do_deploy() {
    # Create deploy folder
    install -d ${DEPLOYDIR}/${PN}

    # Copy license file to distribute
    install -d ${DEPLOYDIR}/${PN}/License
    install -m 0644 ${WORKDIR}/LICENSE-index.txt ${DEPLOYDIR}/${PN}
    install -m 0644 ${WORKDIR}/LICENSE.MIT ${DEPLOYDIR}/${PN}/License

    # Copy to deploy folder
    install -m 0644 ${WORKDIR}/burn.py ${DEPLOYDIR}/${PN}
    install -m 0755 ${WORKDIR}/run.sh ${DEPLOYDIR}/${PN}
    install -m 0644 ${WORKDIR}/run.bat ${DEPLOYDIR}/${PN}
    install -m 0644 ${DEPLOY_DIR}/images/${MACHINE}/u-boot-elf-cm33-ironhide.shdr ${DEPLOYDIR}/${PN}
    install -m 0644 ${DEPLOY_DIR}/images/${MACHINE}/u-boot-elf-cm33-ironhide.srec ${DEPLOYDIR}/${PN}
    install -m 0644 ${DEPLOY_DIR}/images/${MACHINE}/u-boot-elf-cm33-ironhide-scif-download.srec ${DEPLOYDIR}/${PN}
    cp -r  ${DEPLOY_DIR}/licenses/${MACHINE_LIC}/u-boot-cm33 ${DEPLOYDIR}/${PN}/License/u-boot-cm33_licenses

    # install embedded python binary for Windows environment
    PYTHON_DIR=${DEPLOYDIR}/${PN}/python-embed-amd64
    cd ${WORKDIR}
    wget -qc https://www.python.org/ftp/python/3.13.4/python-3.13.4-embed-amd64.zip
    wget -qc https://bootstrap.pypa.io/get-pip.py
    install -d ${PYTHON_DIR}
    unzip -qo ${WORKDIR}/python-3.13.4-embed-amd64.zip -d ${PYTHON_DIR}
    sed -i ${PYTHON_DIR}/*._pth -e "s/.*import site/import site/"
    install -m 0644 ${WORKDIR}/get-pip.py ${PYTHON_DIR}
}

addtask deploy before do_build after do_compile

