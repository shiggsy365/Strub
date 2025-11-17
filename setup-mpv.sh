#!/bin/bash

# MPV Library Setup Helper Script
# This script helps you download and setup MPV libraries for the Android app

set -e

PROJECT_DIR=$(pwd)
JNILIBS_DIR="$PROJECT_DIR/app/src/main/jniLibs"

echo "==============================================="
echo "Stremio MPV Player - Library Setup Helper"
echo "==============================================="
echo ""

# Function to download from releases
download_from_release() {
    echo "Downloading pre-built MPV libraries..."
    
    # Create temp directory
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"
    
    # Get latest release URL
    RELEASE_URL="https://github.com/mpv-android/mpv-android/releases/latest"
    
    echo "Please download the latest MPV Android APK from:"
    echo "$RELEASE_URL"
    echo ""
    echo "Then extract it and copy the lib/ folder contents to:"
    echo "$JNILIBS_DIR"
    echo ""
    echo "Or run these commands after downloading mpv-android.apk:"
    echo "  unzip mpv-android.apk -d mpv-temp"
    echo "  mkdir -p $JNILIBS_DIR"
    echo "  cp -r mpv-temp/lib/* $JNILIBS_DIR/"
    
    cd "$PROJECT_DIR"
}

# Function to build from source
build_from_source() {
    echo "Building MPV from source..."
    echo "WARNING: This will take significant time and requires:"
    echo "  - git, python3, meson, ninja-build, wget"
    echo "  - Several GB of disk space"
    echo ""
    read -p "Continue? (y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        return
    fi
    
    # Create temp directory
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"
    
    echo "Cloning mpv-android repository..."
    git clone https://github.com/mpv-android/mpv-android.git
    cd mpv-android
    
    echo "Building MPV (this will take a while)..."
    ./buildall.sh
    
    echo "Copying libraries to project..."
    mkdir -p "$JNILIBS_DIR"
    cp -r buildscripts/sdk/gradle/deps/lib/* "$JNILIBS_DIR/"
    
    echo "Build complete!"
    cd "$PROJECT_DIR"
    rm -rf "$TEMP_DIR"
}

# Function to verify setup
verify_setup() {
    echo "Verifying library setup..."
    echo ""
    
    ARCHITECTURES=("arm64-v8a" "armeabi-v7a" "x86" "x86_64")
    ALL_FOUND=true
    
    for arch in "${ARCHITECTURES[@]}"; do
        LIB_PATH="$JNILIBS_DIR/$arch/libmpv.so"
        if [ -f "$LIB_PATH" ]; then
            SIZE=$(du -h "$LIB_PATH" | cut -f1)
            echo "✓ Found $arch/libmpv.so ($SIZE)"
        else
            echo "✗ Missing $arch/libmpv.so"
            ALL_FOUND=false
        fi
    done
    
    echo ""
    if [ "$ALL_FOUND" = true ]; then
        echo "✓ All libraries found! You're ready to build the app."
    else
        echo "✗ Some libraries are missing. Please complete the setup."
    fi
}

# Main menu
echo "Select an option:"
echo "1) Download pre-built libraries (recommended)"
echo "2) Build from source (advanced)"
echo "3) Verify current setup"
echo "4) Exit"
echo ""
read -p "Enter choice [1-4]: " choice

case $choice in
    1)
        download_from_release
        ;;
    2)
        build_from_source
        verify_setup
        ;;
    3)
        verify_setup
        ;;
    4)
        echo "Exiting..."
        exit 0
        ;;
    *)
        echo "Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "Setup helper completed!"
echo ""
echo "Next steps:"
echo "1. Open project in Android Studio"
echo "2. Sync Gradle files"
echo "3. Build and run the app"
echo ""
echo "For detailed instructions, see SETUP.md and README.md"
