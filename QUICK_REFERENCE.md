# Quick Reference Card

## Essential Information

### What You Have
✓ Complete Android Studio project
✓ MPV player integration (native C++)
✓ Stremio addon support
✓ Material Design UI
✓ Playback controls

### What You Need
⚠️ **MPV libraries (libmpv.so)** - CRITICAL!
   Download from: https://github.com/mpv-android/mpv-android/releases
   OR run: ./setup-mpv.sh

⚠️ Android NDK + CMake
   Install via: Android Studio → SDK Manager → SDK Tools

### Quick Setup (5 minutes)

1. **Extract the ZIP file**
   ```bash
   unzip StremioMPVPlayer.zip
   cd StremioMPVPlayer
   ```

2. **Setup MPV libraries**
   ```bash
   chmod +x setup-mpv.sh
   ./setup-mpv.sh
   ```
   OR manually download and extract to:
   `app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86,x86_64}/libmpv.so`

3. **Open in Android Studio**
   - File → Open → Select StremioMPVPlayer folder
   - Wait for Gradle sync

4. **Build and Run**
   - Build → Make Project
   - Run → Run 'app'

### Test Addons

```
https://v3-cinemeta.strem.io
https://v3-catalogs.strem.io
https://torrentio.strem.fun/manifest.json
```

### File Structure

```
StremioMPVPlayer/
├── README.md              ← Full documentation
├── SETUP.md               ← Quick setup guide
├── PROJECT_OVERVIEW.md    ← Technical details
├── setup-mpv.sh           ← MPV setup helper
├── app/
│   ├── src/main/
│   │   ├── java/          ← Kotlin source code
│   │   ├── cpp/           ← Native C++ code
│   │   ├── res/           ← UI resources
│   │   └── jniLibs/       ← Native libraries (YOU MUST ADD)
│   └── build.gradle       ← App configuration
└── build.gradle           ← Project configuration
```

### Key Classes

- **MainActivity.kt**: Browse addons and content
- **PlayerActivity.kt**: Video playback
- **StremioClient.kt**: Network API client
- **MPVPlayer.kt**: Native player wrapper
- **mpv_player.cpp**: C++ JNI bridge

### Common Commands

```bash
# Build
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# View logs
adb logcat | grep -E "MPVPlayer|StremioClient"

# Clean build
./gradlew clean
```

### Troubleshooting One-Liners

**Library not found?**
→ Check `app/src/main/jniLibs/` has libmpv.so

**CMake error?**
→ Install NDK via SDK Manager

**Cannot connect?**
→ Check internet + addon URL

**Black screen?**
→ Invalid stream URL or codec issue

### Documentation Files

1. **README.md** - Complete guide (7000+ words)
2. **SETUP.md** - Quick setup steps
3. **PROJECT_OVERVIEW.md** - Architecture details
4. **This file** - Quick reference

### Next Steps After Setup

1. Add image loading (Glide/Coil)
2. Customize MPV settings
3. Add subtitle support
4. Implement PiP mode
5. Add background playback

### Getting Help

Check in order:
1. SETUP.md troubleshooting section
2. README.md detailed docs
3. Logcat output (`adb logcat`)
4. MPV documentation (mpv.io)
5. Stremio addon docs

### Important Notes

⚠️ **This app requires libmpv.so files to work!**
⚠️ MPV is GPL licensed - understand license implications
⚠️ Respect content provider terms of service
⚠️ Test with legal, authorized content only

### Build Requirements

- Android Studio Arctic Fox+
- JDK 8 or higher
- Android SDK 24+ (Android 7.0+)
- NDK 25+ with CMake
- ~2GB free disk space

### Runtime Requirements

- Android 7.0+ device/emulator
- Internet connection
- ~50MB app size (varies with ABIs)

---
Created: November 2024
Project: Stremio MPV Player for Android
