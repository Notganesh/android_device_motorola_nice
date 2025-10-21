# ROM source patches

color="\033[0;32m"
end="\033[0m"

echo -e "${color}Applying patches${end}"
sleep 1

# source
git clone https://github.com/Motorola-Edge-60-Fusion-Development/kernel_motorola_mt6878.git kernel/motorola/mt6878

# Kernel
git clone https://github.com/Notganesh/device_motorola_nice-kernel.git device/motorola/nice-kernel

# Hardware
git clone https://github.com/Moto-MT6879-Dev/hardware_mediatek.git hardware/mediatek

# Motorola
git clone https://github.com/Moto-MT6879-Dev/hardware_motorola.git hardware/motorola

# ims
git clone https://github.com/techyminati/android_vendor_mediatek_ims vendor/mediatek/ims

# Sepolicy
git clone https://github.com/Notganesh/device_mediatek_sepolicy_vndr.git device/mediatek/sepolicy_vndr

# Vendor
git clone https://gitea.com/Notganesh/android_vendor_motorola_nice.git vendor/motorola/nice
