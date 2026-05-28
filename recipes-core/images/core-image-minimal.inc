# Add overrides for selection 32 bit or 64 bit target
OVERRIDES .= ":${TARGET_SYS}"

# Basic packages
IMAGE_INSTALL:append = " \
    ${CORE_IMAGE_BASE_INSTALL} \
    bash \
"

#Installation for ethtool
IMAGE_INSTALL:append = " ethtool"

# Enable package manager
EXTRA_IMAGE_FEATURES += "package-management"

# Enable ssh feature
EXTRA_IMAGE_FEATURES:append = " ssh-server-openssh"

# Fix SDK build error with apt package manger
TOOLCHAIN_TARGET_TASK:remove = "target-sdk-provides-dummy"

