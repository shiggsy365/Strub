package com.example.stremiompvplayer;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\r\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0011\u001a\u00020\u000e2\u0006\u0010\u0012\u001a\u00020\u0013H\u0002J\b\u0010\u0014\u001a\u00020\u0015H\u0003J\u0012\u0010\u0016\u001a\u00020\u00152\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u0014J\b\u0010\u0019\u001a\u00020\u0015H\u0014J\b\u0010\u001a\u001a\u00020\u0015H\u0014J\b\u0010\u001b\u001a\u00020\u0015H\u0014J\b\u0010\u001c\u001a\u00020\u0015H\u0002J\b\u0010\u001d\u001a\u00020\u0015H\u0003J\b\u0010\u001e\u001a\u00020\u0015H\u0003J\u0010\u0010\u001f\u001a\u00020\u00152\u0006\u0010 \u001a\u00020\u000eH\u0002J\b\u0010!\u001a\u00020\u0015H\u0003J\u0010\u0010\"\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\fH\u0002J\b\u0010$\u001a\u00020\u0015H\u0002R\u0010\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0004\n\u0002\u0010\u0005R\u0010\u0010\u0006\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0005R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000f\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006%"}, d2 = {"Lcom/example/stremiompvplayer/PlayerActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "error/NonExistentClass", "Lerror/NonExistentClass;", "exoPlayer", "handler", "Landroid/os/Handler;", "hideControlsRunnable", "Ljava/lang/Runnable;", "isControlsVisible", "", "streamTitle", "", "streamUrl", "updateProgressRunnable", "formatTime", "seconds", "", "hideControls", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "onPause", "onResume", "setupControls", "setupExoPlayer", "showControls", "showError", "message", "toggleControls", "updatePlayPauseButton", "playing", "updateProgress", "app_debug"})
public final class PlayerActivity extends androidx.appcompat.app.AppCompatActivity {
    private error.NonExistentClass binding;
    @org.jetbrains.annotations.Nullable()
    private error.NonExistentClass exoPlayer;
    @org.jetbrains.annotations.NotNull()
    private final android.os.Handler handler = null;
    private boolean isControlsVisible = false;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String streamUrl;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String streamTitle;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.Runnable hideControlsRunnable = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.Runnable updateProgressRunnable = null;
    
    public PlayerActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @androidx.annotation.OptIn(markerClass = {UnstableApi.class})
    private final void setupExoPlayer() {
    }
    
    private final void setupControls() {
    }
    
    @androidx.annotation.OptIn(markerClass = {UnstableApi.class})
    private final void toggleControls() {
    }
    
    @androidx.annotation.OptIn(markerClass = {UnstableApi.class})
    private final void showControls() {
    }
    
    @androidx.annotation.OptIn(markerClass = {UnstableApi.class})
    private final void hideControls() {
    }
    
    private final void updateProgress() {
    }
    
    private final void updatePlayPauseButton(boolean playing) {
    }
    
    private final java.lang.String formatTime(int seconds) {
        return null;
    }
    
    private final void showError(java.lang.String message) {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    @java.lang.Override()
    protected void onPause() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}