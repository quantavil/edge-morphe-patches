#!/usr/bin/env bash

# Exit immediately if any command fails
set -e

# Curated, harmonious colors for terminal output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0;0m' # No Color

FORCE_FLAG=""

# Parse flags
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -f|--force)
            FORCE_FLAG="-f"
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Usage: $0 [-f|--force]"
            exit 1
            ;;
    esac
    shift
done

echo -e "${BLUE}==================================================${NC}"
echo -e "${CYAN}🚀 Edge Morphe Patches: One-Click Build & Deploy${NC}"
echo -e "${BLUE}==================================================${NC}"

# 1. Check if ADB device is connected
echo -e "\n${YELLOW}[1/5] Checking connected ADB devices...${NC}"
DEVICES=$(adb devices | tail -n +2 | grep -v "^$" || true)

INSTALL_FLAG=""
DEVICE_SERIAL=""

if [ -z "$DEVICES" ]; then
    echo -e "${YELLOW}⚠️ Warning: No Android devices found via ADB. Patching will proceed, but installation and launch steps will be skipped.${NC}"
else
    DEVICE_SERIAL=$(echo "$DEVICES" | head -n 1 | awk '{print $1}')
    INSTALL_FLAG="-i=$DEVICE_SERIAL"
    echo -e "${GREEN}✅ Found device: $DEVICE_SERIAL. Patched APK will be installed.${NC}"
fi

# 2. Build the patches project
echo -e "\n${YELLOW}[2/5] Compiling patches...${NC}"
./gradlew buildAndroid

# Locate the compiled MPP file
MPP_FILE=$(find patches/build/libs/ -name "patches-*.mpp" ! -name "*-sources.mpp" ! -name "*-javadoc.mpp" | head -n 1)
if [ -z "$MPP_FILE" ]; then
    echo -e "${RED}❌ Error: Could not find compiled .mpp patch file in patches/build/libs/${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Patches compiled successfully: $MPP_FILE${NC}"

# 3. Clean cache/temporary files
echo -e "\n${YELLOW}[3/5] Cleaning temporary Morphe build files...${NC}"
rm -rf morphe-data/tmp/*
echo -e "${GREEN}✅ Temporary files cleaned.${NC}"

# 4. Patch base APK and install
# Note: "Change package name" is disabled because ReAndroid's package name renamer post-processor
# fails on Microsoft Edge's complex layout/color resources (specifically m3_navigation_item_ripple_color.xml).
echo -e "\n${YELLOW}[4/5] Patching edge_base.apk (Applying Telemetry, Copilot, & Play Store updates patches)...${NC}"
if [ ! -f "edge_base.apk" ]; then
    echo -e "${RED}❌ Error: edge_base.apk not found in the root directory.${NC}"
    exit 1
fi

if [ -n "$DEVICE_SERIAL" ]; then
    echo -e "${YELLOW}🗑️ Uninstalling previous Edge packages to prevent signature conflicts...${NC}"
    adb -s "$DEVICE_SERIAL" uninstall com.microsoft.emmx || true
    adb -s "$DEVICE_SERIAL" uninstall com.microsoft.emmx.beta || true
fi

# Enable error handling specific to patching
set +e
java -jar morphe-cli.jar patch \
    $FORCE_FLAG \
    $INSTALL_FLAG \
    -e "Disable Play Store updates" \
    -e "Telemetry elimination" \
    -e "Copilot feature toggle" \
    --patches="$MPP_FILE" \
    -o edge_patched.apk \
    edge_base.apk
PATCH_EXIT_CODE=$?
set -e

if [ $PATCH_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}❌ Error: Patching failed with exit code $PATCH_EXIT_CODE.${NC}"
    echo -e "${YELLOW}💡 Troubleshooting Tip:${NC}"
    echo "If you updated 'edge_base.apk' to a newer version, the patcher might fail because of compatibility metadata."
    echo "You can:"
    echo "1. Run the script with the force flag to bypass checks: ./run_pipeline.sh --force (or -f)"
    echo "2. Or edit the compatible versions target in the patches source code (e.g. Constants configuration files)."
    exit $PATCH_EXIT_CODE
fi

echo -e "${GREEN}✅ Patching completed successfully!${NC}"

# 5. Launch the application on the device (if connected)
if [ -n "$DEVICE_SERIAL" ]; then
    echo -e "\n${YELLOW}[5/5] Launching the patched application...${NC}"
    # Since package name is not changed, it is com.microsoft.emmx or com.microsoft.emmx.beta
    ALL_PACKAGES=$(adb shell pm list packages | grep -oE "com\.microsoft\.emmx(\.beta)?" || true)
    
    if echo "$ALL_PACKAGES" | grep -q "com.microsoft.emmx.beta"; then
        PACKAGE_NAME="com.microsoft.emmx.beta"
    else
        PACKAGE_NAME="com.microsoft.emmx"
    fi

    echo -e "${CYAN}📱 Starting package: $PACKAGE_NAME${NC}"
    adb shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1

    echo -e "${GREEN}🎉 Done! Edge has been launched on your device.${NC}"
else
    echo -e "\n${YELLOW}[5/5] Skipping launch step (no device connected).${NC}"
    echo -e "${GREEN}🎉 Done! The patched APK is available at: $(pwd)/edge_patched.apk${NC}"
fi
echo -e "${BLUE}==================================================${NC}"
