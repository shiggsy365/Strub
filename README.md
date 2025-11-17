# Stremio MPV Player for Android

An Android application that uses MPV player to play streams from Stremio addons and manifests.

## Features

- **Stremio Addon Integration**: Connect to any Stremio addon via manifest URL
- **MPV Player**: High-performance video playback using libmpv
- **Stream Selection**: Browse catalogs and select from available streams
- **Full Playback Controls**: Play, pause, seek, and adjust playback
- **Material Design UI**: Modern, clean interface with dark theme

## Architecture

### Components

1. **MainActivity**: Browse and select content from Stremio addons
2. **PlayerActivity**: Video playback with MPV player
3. **StremioClient**: Network layer for Stremio API communication
4. **MPVPlayer**: Native wrapper for libmpv integration

### Data Flow

```
User Input → MainActivity → StremioClient → Stremio Addon API
                ↓
          Catalog/Streams
                ↓
         PlayerActivity → MPVPlayer (JNI) → libmpv → Video Output
```

## Prerequisites

### Required

1. **Android Studio** Arctic Fox or newer
2. **Android SDK** with minimum SDK 24 (Android 7.0)
3. **Android NDK** (for building native code)
4. **Kotlin** support

### MPV Library Setup

**IMPORTANT**: This app requires libmpv compiled for Android. You have two options:

#### Option 1: Build MPV from Source (Recommended)

1. Clone the MPV Android build repository:
   ```bash
   git clone https://github.com/mpv-android/mpv-android.git
   cd mpv-android
   ```

2. Follow the build instructions to compile libmpv for Android:
   ```bash
   ./buildall.sh
   ```

3. Copy the built libraries to your project:
   ```bash
   mkdir -p app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86,x86_64}
   cp mpv-android/buildscripts/sdk/gradle/deps/lib/*/libmpv.so app/src/main/jniLibs/*/
   ```

4. Copy MPV headers:
   ```bash
   mkdir -p app/src/main/cpp/mpv
   cp -r mpv-android/mpv/libmpv/include/* app/src/main/cpp/mpv/include/
   ```

#### Option 2: Use Pre-built Libraries

Download pre-built MPV libraries from:
- [mpv-android releases](https://github.com/mpv-android/mpv-android/releases)

Extract and place in:
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

## Project Structure

```
StremioMPVPlayer/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/stremiompvplayer/
│   │   │   │   ├── adapters/
│   │   │   │   │   ├── ContentAdapter.kt
│   │   │   │   │   └── StreamAdapter.kt
│   │   │   │   ├── models/
│   │   │   │   │   └── StremioModels.kt
│   │   │   │   ├── network/
│   │   │   │   │   └── StremioClient.kt
│   │   │   │   ├── player/
│   │   │   │   │   └── MPVPlayer.kt
│   │   │   │   ├── viewmodels/
│   │   │   │   │   └── MainViewModel.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── PlayerActivity.kt
│   │   │   ├── cpp/
│   │   │   │   ├── mpv_player.cpp
│   │   │   │   └── CMakeLists.txt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── ...
│   │   │   └── AndroidManifest.xml
│   │   └── ...
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## Building the Project

### 1. Clone/Copy the Project

Place all files in your project directory maintaining the structure shown above.

### 2. Configure build.gradle (app level)

Add NDK configuration to `app/build.gradle`:

```gradle
android {
    // ... other configurations
    
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.18.1"
        }
    }
    
    ndkVersion "25.1.8937393" // Use your installed NDK version
}
```

### 3. Sync Gradle

In Android Studio: File → Sync Project with Gradle Files

### 4. Build the Project

```bash
./gradlew assembleDebug
```

Or in Android Studio: Build → Make Project

## Usage

### 1. Launch the App

Install and launch the app on your Android device or emulator.

### 2. Add a Stremio Addon

Enter an addon URL in the text field. Examples:

**Official Stremio Addons:**
- `https://v3-cinemeta.strem.io` - Movies and TV shows metadata
- `https://v3-catalogs.strem.io` - Catalogs

**Community Addons:**
- `https://torrentio.strem.fun/manifest.json` - Torrent streams
- `https://1fe84bc728af-stremio-addon-manager.baby-beamup.club/manifest.json` - Addon manager

### 3. Browse Content

Tap "Load" to fetch the catalog. Browse through available movies/shows.

### 4. Select and Play

Tap on any item to:
1. Fetch available streams
2. Select a stream (if multiple available)
3. Start playback with MPV player

### 5. Playback Controls

- **Tap screen**: Show/hide controls
- **Play/Pause**: Toggle playback
- **Seek**: Drag the progress bar
- **Rewind/Forward**: Skip 10 seconds backward/forward
- **Back button**: Return to catalog

## Stremio Addon Protocol

### Manifest Structure

```json
{
  "id": "com.example.addon",
  "version": "1.0.0",
  "name": "My Addon",
  "description": "Description of addon",
  "resources": ["catalog", "stream"],
  "types": ["movie", "series"],
  "catalogs": [
    {
      "type": "movie",
      "id": "top",
      "name": "Top Movies"
    }
  ]
}
```

### API Endpoints

- **Manifest**: `GET /manifest.json`
- **Catalog**: `GET /catalog/{type}/{id}.json`
- **Meta**: `GET /meta/{type}/{id}.json`
- **Streams**: `GET /stream/{type}/{id}.json`

### Stream Response

```json
{
  "streams": [
    {
      "title": "1080p",
      "url": "https://example.com/video.mp4"
    }
  ]
}
```

## Supported Stream Types

MPV can play:
- Direct HTTP/HTTPS URLs (MP4, MKV, WebM, etc.)
- HLS streams (m3u8)
- DASH streams
- YouTube URLs (via youtube-dl if configured)

**Note**: Torrent magnet links require additional configuration and are not directly supported by MPV.

## Troubleshooting

### MPV Library Not Found

**Error**: `UnsatisfiedLinkError: dlopen failed: library "libmpv.so" not found`

**Solution**: Ensure libmpv.so is properly placed in `app/src/main/jniLibs/` for all architectures.

### Network Issues

**Error**: Unable to load addon or streams

**Solutions**:
1. Check internet connection
2. Verify addon URL is correct and accessible
3. Check `AndroidManifest.xml` has `INTERNET` permission
4. Ensure `usesCleartextTraffic="true"` for HTTP addons

### Playback Issues

**Error**: Stream won't play or shows black screen

**Solutions**:
1. Verify stream URL is valid and accessible
2. Check MPV logs in Logcat (filter by "MPVPlayer")
3. Try a different stream from the same content
4. Ensure device has codec support for the stream format

### Build Errors

**Error**: CMake/NDK configuration errors

**Solutions**:
1. Install NDK via SDK Manager
2. Update NDK version in `build.gradle`
3. Verify CMakeLists.txt paths are correct
4. Clean and rebuild: Build → Clean Project, then Build → Rebuild Project

## Customization

### Adding Image Loading

Install Glide or Coil for poster images:

```gradle
// Add to app/build.gradle
dependencies {
    implementation 'com.github.bumptech.glide:glide:4.16.0'
}
```

Update ContentAdapter:

```kotlin
import com.bumptech.glide.Glide

// In ViewHolder.bind()
Glide.with(itemView.context)
    .load(item.poster)
    .placeholder(R.drawable.placeholder)
    .into(posterImage)
```

### Configuring MPV Options

Modify MPVPlayer initialization:

```kotlin
// In MPVPlayer.initialize()
nativeSetOption(mpvHandle, "cache", "yes")
nativeSetOption(mpvHandle, "cache-secs", "300") // 5 minutes
nativeSetOption(mpvHandle, "hwdec", "mediacodec") // Use Android MediaCodec
```

### Adding Subtitles Support

```kotlin
// In MPVPlayer
fun addSubtitle(url: String) {
    nativeCommand(mpvHandle, arrayOf("sub-add", url))
}

fun setSubtitleTrack(index: Int) {
    nativeSetPropertyInt(mpvHandle, "sid", index)
}
```

## Performance Optimization

1. **Hardware Decoding**: Enable MediaCodec for better performance
2. **Cache Size**: Adjust cache settings based on network speed
3. **Buffer Size**: Configure demuxer buffer for smoother playback
4. **Image Loading**: Use image caching libraries (Glide/Coil)

## Security Considerations

1. **HTTPS**: Prefer HTTPS addons over HTTP
2. **URL Validation**: Validate and sanitize addon URLs
3. **Content Verification**: Be cautious with untrusted addons
4. **Network Security**: Consider implementing certificate pinning

## Known Limitations

1. **Torrent Support**: Requires additional torrent client integration
2. **DRM Content**: Not supported by MPV
3. **Some Codecs**: May require additional compilation flags
4. **Background Playback**: Not implemented (can be added)

## Contributing

Contributions are welcome! Areas for improvement:

- [ ] Background playback support
- [ ] Picture-in-Picture mode
- [ ] Playlist management
- [ ] Subtitle customization UI
- [ ] Audio track selection
- [ ] Chromecast support
- [ ] Download for offline playback

## License

This project is for educational purposes. Ensure compliance with:
- MPV's GPL license
- Stremio's addon protocol terms
- Content provider's terms of service

## Resources

- [MPV Documentation](https://mpv.io/manual/master/)
- [Stremio Addon SDK](https://github.com/Stremio/stremio-addon-sdk)
- [MPV Android](https://github.com/mpv-android/mpv-android)
- [Stremio Protocol Documentation](https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/protocol.md)

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review MPV and Stremio documentation
3. Check Android Logcat for detailed error messages

## Acknowledgments

- **MPV Project**: For the excellent media player
- **Stremio Team**: For the addon protocol and ecosystem
- **Android Community**: For libraries and tools used
