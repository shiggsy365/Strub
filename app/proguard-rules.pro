# Add project specific ProGuard rules here.

# Keep MPV native methods
-keep class com.example.stremiompvplayer.player.MPVPlayer {
    native <methods>;
    private void eventCallback(int, java.lang.String);
}

# Keep model classes for Gson
-keep class com.example.stremiompvplayer.models.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
