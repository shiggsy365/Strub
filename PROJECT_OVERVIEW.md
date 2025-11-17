# Stremio MPV Player - Project Overview

## What This App Does

This Android application allows users to:
1. Connect to Stremio addon servers
2. Browse available movies and TV shows
3. Select and play video streams using the MPV player
4. Enjoy high-quality playback with full controls

## Key Technologies

- **Kotlin**: Modern Android development
- **MPV (libmpv)**: Powerful, open-source media player
- **Stremio Protocol**: Access to vast content ecosystem
- **Material Design 3**: Modern, intuitive UI
- **Coroutines**: Efficient async operations
- **OkHttp**: Robust networking
- **JNI/NDK**: Native C++ integration for MPV

## App Architecture

### Layers

1. **Presentation Layer** (Activities/ViewModels)
   - MainActivity: Content browsing
   - PlayerActivity: Video playback
   - MainViewModel: Business logic and state

2. **Network Layer** (StremioClient)
   - Addon manifest retrieval
   - Catalog browsing
   - Stream fetching

3. **Player Layer** (MPVPlayer + Native)
   - JNI bridge to libmpv
   - Playback control
   - Surface rendering

4. **Data Layer** (Models)
   - Stremio protocol data structures
   - Gson serialization

## File Organization

```
StremioMPVPlayer/
├── app/
│   ├── build.gradle              # App dependencies and config
│   ├── proguard-rules.pro        # Code obfuscation rules
│   └── src/main/
│       ├── AndroidManifest.xml   # App permissions and components
│       ├── java/com/example/stremiompvplayer/
│       │   ├── MainActivity.kt           # Main screen
│       │   ├── PlayerActivity.kt         # Player screen
│       │   ├── adapters/
│       │   │   ├── ContentAdapter.kt     # Content list adapter
│       │   │   └── StreamAdapter.kt      # Stream selection adapter
│       │   ├── models/
│       │   │   └── StremioModels.kt      # Data models
│       │   ├── network/
│       │   │   └── StremioClient.kt      # API client
│       │   ├── player/
│       │   │   └── MPVPlayer.kt          # MPV wrapper
│       │   └── viewmodels/
│       │       └── MainViewModel.kt      # Business logic
│       ├── cpp/
│       │   ├── mpv_player.cpp            # Native MPV bridge
│       │   └── CMakeLists.txt            # Native build config
│       ├── res/
│       │   ├── layout/                   # UI layouts
│       │   ├── values/                   # Strings, themes
│       │   └── ...
│       └── jniLibs/                      # Native libraries
│           ├── arm64-v8a/
│           ├── armeabi-v7a/
│           ├── x86/
│           └── x86_64/
├── build.gradle                  # Project-level build config
├── settings.gradle               # Project settings
├── gradle.properties             # Gradle properties
├── README.md                     # Full documentation
├── SETUP.md                      # Quick setup guide
└── setup-mpv.sh                  # MPV setup helper script
```

## Core Features Explained

### 1. Addon Connection

```kotlin
// User enters addon URL
viewModel.loadAddon("https://v3-cinemeta.strem.io")

// App fetches manifest
StremioClient.getManifest(url) → AddonManifest

// App loads catalog
StremioClient.getCatalog(type, id) → List<MetaPreview>
```

### 2. Content Browsing

- RecyclerView displays content items
- Each item shows poster, title, release info
- Tapping item fetches available streams

### 3. Stream Selection

```kotlin
// Fetch streams for selected content
streams = StremioClient.getStreams(type, id)

// Filter for direct URLs (MPV compatible)
playableStreams = streams.filter { it.url != null }

// Show selection dialog or play directly
```

### 4. Video Playback

```kotlin
// Initialize MPV
mpvPlayer.initialize()
mpvPlayer.setSurface(surfaceHolder.surface)

// Load and play stream
mpvPlayer.loadFile(streamUrl)
mpvPlayer.play()

// Control playback
mpvPlayer.pause()
mpvPlayer.seek(10)
mpvPlayer.setPosition(newPosition)
```

## Critical Setup Requirements

### 1. MPV Libraries (REQUIRED)

The app **will not work** without libmpv.so files. Options:

- **Download**: Get from mpv-android releases
- **Build**: Compile from mpv-android source

Place in: `app/src/main/jniLibs/{architecture}/libmpv.so`

### 2. Android NDK (REQUIRED for building)

Install via Android Studio SDK Manager:
- NDK (Side by side)
- CMake

### 3. Network Permissions

Already configured in AndroidManifest.xml:
- INTERNET
- ACCESS_NETWORK_STATE
- WAKE_LOCK

## Stremio Addon Examples

### Official Addons

1. **Cinemeta** (Metadata)
   - URL: `https://v3-cinemeta.strem.io`
   - Provides: Movie/TV metadata, posters, descriptions

2. **Catalogs** (Content Discovery)
   - URL: `https://v3-catalogs.strem.io`
   - Provides: Various content catalogs

### Community Addons

1. **Torrentio** (Torrent Streams)
   - URL: `https://torrentio.strem.fun/manifest.json`
   - Note: Requires torrent client for magnets

2. **YouTube** (YouTube Videos)
   - Various YouTube addon implementations
   - Direct video URLs

## Development Workflow

1. **Edit Code** in Android Studio
2. **Sync Gradle** after changes
3. **Build Project** (Ctrl+F9)
4. **Run on Device** (Shift+F10)
5. **Check Logs** (Logcat)

## Testing Strategy

### Unit Tests
- StremioClient API parsing
- Model serialization/deserialization
- URL validation

### Integration Tests
- Network connectivity
- Addon communication
- Stream retrieval

### Manual Testing
1. Load different addons
2. Browse catalogs
3. Play various stream types
4. Test playback controls
5. Handle edge cases (no streams, errors)

## Performance Considerations

### Network
- Use coroutines for async operations
- Implement timeout handling
- Cache responses where appropriate

### Player
- Hardware decoding (MediaCodec)
- Efficient buffer management
- Surface rendering optimization

### UI
- RecyclerView for efficient lists
- Image loading optimization (add Glide/Coil)
- Background thread for heavy operations

## Common Issues & Solutions

### Build Issues
- **Problem**: CMake errors
- **Solution**: Install NDK via SDK Manager

### Runtime Issues
- **Problem**: Library not found
- **Solution**: Check jniLibs structure

### Network Issues
- **Problem**: Cannot load addon
- **Solution**: Check URL, internet, permissions

### Playback Issues
- **Problem**: Black screen
- **Solution**: Verify stream URL, check codecs

## Extending the App

### Add Subtitle Support
1. Extend MPVPlayer with subtitle methods
2. Add UI for subtitle selection
3. Implement subtitle download/loading

### Add Picture-in-Picture
1. Configure AndroidManifest
2. Implement PiP mode in PlayerActivity
3. Handle lifecycle changes

### Add Downloads
1. Implement DownloadManager integration
2. Store videos locally
3. Build offline playback

### Add Chromecast
1. Add Cast SDK dependency
2. Implement Cast integration
3. Handle remote playback

## License & Legal

- **MPV**: GPL v2+ (requires compliance)
- **App Code**: Specify your license
- **Content**: Respect provider terms
- **Addons**: Check individual addon licenses

## Resources

### Documentation
- MPV: https://mpv.io/manual/master/
- Stremio: https://github.com/Stremio/stremio-addon-sdk
- Android: https://developer.android.com

### Tools
- Android Studio
- ADB (Android Debug Bridge)
- Logcat for debugging

### Community
- MPV Android: https://github.com/mpv-android
- Stremio Addons: https://stremio-addons.netlify.app

## Project Status

This is a **functional prototype** demonstrating:
✓ Stremio addon integration
✓ MPV player integration
✓ Basic playback controls
✓ Material Design UI

**Not Yet Implemented**:
- Background playback
- PiP mode
- Subtitle customization
- Audio track selection
- Playlist management
- Download support

## Getting Started

1. Read **SETUP.md** for quick setup
2. Read **README.md** for full documentation
3. Run `./setup-mpv.sh` to setup libraries
4. Open in Android Studio
5. Build and run!

## Support

For issues:
1. Check documentation
2. Review Logcat logs
3. Verify library setup
4. Check network connectivity
5. Test with known-good addons

Happy coding! 🚀
