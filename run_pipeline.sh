#!/usr/bin/env bash

# Exit immediately if any command fails
set -e

# ANSI colors for terminal output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m' # Reset

FORCE_FLAG=""
TARGET="stable"

# Parse flags
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -f|--force)
            FORCE_FLAG="-f"
            ;;
        --canary)
            TARGET="canary"
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Usage: $0 [-f|--force] [--canary]"
            exit 1
            ;;
    esac
    shift
done

COMPAT_FILE="patches/src/main/kotlin/app/morphe/patches/all/misc/EdgeCompatibility.kt"

if [ "$TARGET" = "canary" ]; then
    COMPAT_MARKER="EDGE_CANARY_COMPATIBILITY"
    BASE_APKS=("edge_canary_base.apkm" "edge_canary_base.apk")
    PREFERRED_PACKAGE="com.microsoft.emmx.canary"
    OUTPUT_PREFIX="edge-canary-patched"
else
    COMPAT_MARKER="EDGE_COMPATIBILITY"
    BASE_APKS=("edge_base.apkm" "edge_base.apk")
    PREFERRED_PACKAGE="com.microsoft.emmx"
    OUTPUT_PREFIX="edge-patched"
fi

# The Morphe CLI requires Java 21 or newer.
JAVA_BIN="${JAVA_BIN:-}"
if [ -z "$JAVA_BIN" ]; then
    for candidate in \
        /usr/lib/jvm/java-27-openjdk/bin/java \
        /usr/lib/jvm/java-26-openjdk/bin/java \
        /usr/lib/jvm/java-25-openjdk/bin/java \
        /usr/lib/jvm/java-24-openjdk/bin/java \
        /usr/lib/jvm/java-23-openjdk/bin/java \
        /usr/lib/jvm/java-22-openjdk/bin/java \
        /usr/lib/jvm/java-21-openjdk/bin/java; do
        if [ -x "$candidate" ]; then
            JAVA_BIN="$candidate"
            break
        fi
    done
fi
JAVA_BIN="${JAVA_BIN:-java}"

JAVA_MAJOR=$("$JAVA_BIN" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1)
if [ -z "$JAVA_MAJOR" ] || [ "$JAVA_MAJOR" -lt 21 ]; then
    echo -e "${RED}❌ Error: Morphe CLI needs Java 21 or newer (found: $JAVA_BIN).${NC}"
    echo "Set JAVA_BIN to a Java 21+ executable and re-run."
    exit 1
fi

echo -e "${BLUE}==================================================${NC}"
echo -e "${CYAN}🚀 Edge Morphe Patches: One-Click Build & Deploy (${TARGET})${NC}"
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
MPP_FILE=$(realpath "$MPP_FILE")
echo -e "${GREEN}✅ Patches compiled successfully: $MPP_FILE${NC}"

# 3. Clean cache/temporary files
echo -e "\n${YELLOW}[3/5] Cleaning temporary Morphe build files...${NC}"
rm -rf morphe-data/tmp/*
echo -e "${GREEN}✅ Temporary files cleaned.${NC}"

# 4. Patch base APK and install
# Note: "Change package name" is disabled because ReAndroid's package name renamer post-processor
# fails on Microsoft Edge's complex layout/color resources (specifically m3_navigation_item_ripple_color.xml).
BASE_APK=""
for candidate in "${BASE_APKS[@]}"; do
    if [ -f "$candidate" ]; then
        BASE_APK="$candidate"
        break
    fi
done

if [ -z "$BASE_APK" ]; then
    echo -e "${RED}❌ Error: No base APK found. Expected one of: ${BASE_APKS[*]}${NC}"
    exit 1
fi

# Morphe CLI 2.1.0 mis-resolves relative input paths when staging split APK bundles
# (broken ApkBundle.loadApkDirectory). Pass absolute paths until the bundled CLI includes 2.1.1.
BASE_APK=$(realpath "$BASE_APK")

echo -e "\n${YELLOW}[4/5] Patching $BASE_APK (Telemetry, Copilot, Play Store updates & news notifications)...${NC}"

if [ -n "$DEVICE_SERIAL" ]; then
    echo -e "${YELLOW}🗑️ Uninstalling previous Edge packages to prevent signature conflicts...${NC}"
    adb -s "$DEVICE_SERIAL" uninstall com.microsoft.emmx || true
    adb -s "$DEVICE_SERIAL" uninstall com.microsoft.emmx.beta || true
    adb -s "$DEVICE_SERIAL" uninstall com.microsoft.emmx.canary || true
fi

EDGE_VERSION=$(awk "/$COMPAT_MARKER = Compatibility/,/^\)/" "$COMPAT_FILE" | grep -oP 'version\s*=\s*"\K[0-9.]+' | head -n 1)
if [ -z "$EDGE_VERSION" ]; then
    echo -e "${RED}❌ Error: Could not read the target version from $COMPAT_FILE${NC}"
    exit 1
fi
OUTPUT_APK="$(realpath -m "$(pwd)/${OUTPUT_PREFIX}-${EDGE_VERSION}-arm64.apk")"

# Enable error handling specific to patching
set +e
"$JAVA_BIN" -jar morphe-cli.jar patch \
    $FORCE_FLAG \
    $INSTALL_FLAG \
    -e "Disable Play Store updates" \
    -e "Telemetry elimination" \
    -e "Copilot feature toggle" \
    -e "Disable news notifications" \
    -p "$MPP_FILE" \
    -o "$OUTPUT_APK" \
    "$BASE_APK"
PATCH_EXIT_CODE=$?
set -e

if [ $PATCH_EXIT_CODE -ne 0 ]; then
    echo -e "${RED}❌ Error: Patching failed with exit code $PATCH_EXIT_CODE.${NC}"
    echo -e "${YELLOW}💡 Troubleshooting Tip:${NC}"
    echo "If you updated the base APK to a newer version, the patcher might fail because of compatibility metadata."
    echo "You can:"
    echo "1. Run the script with the force flag to bypass checks: ./run_pipeline.sh --force (or -f)"
    echo "2. Or edit the compatible versions target in the patches source code (e.g. Constants configuration files)."
    exit $PATCH_EXIT_CODE
fi

echo -e "${GREEN}✅ Patching completed successfully!${NC}"

# 5. Launch the application on the device (if connected)
if [ -n "$DEVICE_SERIAL" ]; then
    echo -e "\n${YELLOW}[5/5] Launching the patched application...${NC}"
    ALL_PACKAGES=$(adb shell pm list packages | grep -oE "com\.microsoft\.emmx(\.beta|\.canary)?" || true)

    if echo "$ALL_PACKAGES" | grep -q "$PREFERRED_PACKAGE"; then
        PACKAGE_NAME="$PREFERRED_PACKAGE"
    elif echo "$ALL_PACKAGES" | grep -q "com.microsoft.emmx.canary"; then
        PACKAGE_NAME="com.microsoft.emmx.canary"
    elif echo "$ALL_PACKAGES" | grep -q "com.microsoft.emmx.beta"; then
        PACKAGE_NAME="com.microsoft.emmx.beta"
    else
        PACKAGE_NAME="com.microsoft.emmx"
    fi

    echo -e "${CYAN}📱 Starting package: $PACKAGE_NAME${NC}"
    adb shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1

    echo -e "${GREEN}🎉 Done! Edge has been launched on your device.${NC}"
    echo -e "${GREEN}📁 Patched APK: $OUTPUT_APK${NC}"
else
    echo -e "\n${YELLOW}[5/5] Skipping launch step (no device connected).${NC}"
    echo -e "${GREEN}🎉 Done! The patched APK is available at: $OUTPUT_APK${NC}"
fi
echo -e "${BLUE}==================================================${NC}"
