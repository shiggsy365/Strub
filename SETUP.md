# Quick Setup Guide

## Step 1: MPV Library Setup

This is the **most critical step**. The app will not work without libmpv.

### Option A: Download Pre-built Libraries (Easiest)

1. Go to: https://github.com/mpv-android/mpv-android/releases
2. Download the latest release APK
3. Extract the APK (it's just a ZIP file):
   ```bash
   unzip mpv-android-*.apk -d mpv-temp
   ```
4. Copy libraries to your project:
   ```bash
   mkdir -p app/src/main/jniLibs
   cp -r mpv-temp/lib/* app/src/main/jniLibs/
   ```

### Option B: Build from Source (Advanced)

1. Clone mpv-android:
   ```bash
   git clone https://github.com/mpv-android/mpv-android.git
   cd mpv-android
   ```

2. Install dependencies (Ubuntu/Debian):
   ```bash
   sudo apt-get install git python3 meson ninja-build wget
   ```

3. Build:
   ```bash
   ./buildall.sh
   ```

4. Copy to project:
   ```bash
   cp -r buildscripts/sdk/gradle/deps/lib/* /path/to/StremioMPVPlayer/app/src/main/jniLibs/
   ```

## Step 2: Configure NDK in Android Studio

1. Open Android Studio
2. Go to: Tools → SDK Manager → SDK Tools
3. Check "NDK (Side by side)" and "CMake"
4. Click Apply to install

## Step 3: Update Build Configuration

Edit `app/build.gradle` and add:

```gradle
android {
    // ... existing config
    
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.18.1"
        }
    }
    
    ndkVersion "25.1.8937393"
}
```

## Step 4: Verify Library Structure

Your project should have this structure:

```
app/src/main/jniLibs/
├── arm64-v8a/
│   └── libmpv.so
├── armeabi-v7a/
│   └── libmpv.so
├── x86/
│   └── libmpv.so
└── x86_64/
    └── libmpv.so
```

## Step 5: Build and Run

1. Sync Gradle: File → Sync Project with Gradle Files
2. Build: Build → Make Project
3. Run: Run → Run 'app'

## Troubleshooting

### "libmpv.so not found"
- Check files are in `jniLibs/` folder
- Verify correct architecture folders
- Clean and rebuild project

### "CMake Error"
- Install CMake via SDK Manager
- Check NDK version matches build.gradle
- Update NDK path in local.properties

### "Cannot connect to addon"
- Check internet connection
- Try example URL: `https://v3-cinemeta.strem.io`
- Check Logcat for network errors

## Testing

1. Launch app
2. Enter addon URL: `https://v3-cinemeta.strem.io`
3. Tap "Load"
4. Browse movies/shows
5. Tap any item to play

## Next Steps

- Read full README.md for detailed documentation
- Check example Stremio addons
- Customize player settings
- Add image loading library (Glide/Coil)

## Getting Help

Check the logs:
```bash
adb logcat | grep -E "MPVPlayer|StremioClient"
```

Common issues are usually:
1. Missing libmpv.so files
2. Network connectivity
3. Invalid addon URLs
