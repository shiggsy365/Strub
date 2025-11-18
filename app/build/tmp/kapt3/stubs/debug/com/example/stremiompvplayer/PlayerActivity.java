package com.example.stremiompvplayer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import com.example.stremiompvplayer.databinding.ActivityPlayerBinding;
import com.example.stremiompvplayer.models.MetaItem;
import com.example.stremiompvplayer.models.Stream;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0011\u001a\u00020\u0012H\u0002J\b\u0010\u0013\u001a\u00020\u0012H\u0003J\u0012\u0010\u0014\u001a\u00020\u00122\b\u0010\u0015\u001a\u0004\u0018\u00010\u0016H\u0014J\b\u0010\u0017\u001a\u00020\u0012H\u0017J\b\u0010\u0018\u001a\u00020\u0012H\u0017J\b\u0010\u0019\u001a\u00020\u0012H\u0016J\b\u0010\u001a\u001a\u00020\u0012H\u0017J\u0010\u0010\u001b\u001a\u00020\u00122\u0006\u0010\u001c\u001a\u00020\u001dH\u0016J\b\u0010\u001e\u001a\u00020\u0012H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\t\u001a\u0004\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0010\u001a\u0004\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2 = {"Lcom/example/stremiompvplayer/PlayerActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/example/stremiompvplayer/databinding/ActivityPlayerBinding;", "currentMeta", "Lcom/example/stremiompvplayer/models/MetaItem;", "currentPosition", "", "currentStream", "Lcom/example/stremiompvplayer/models/Stream;", "currentStreamUrl", "", "nextEpisode", "player", "Landroidx/media3/exoplayer/ExoPlayer;", "prevEpisode", "hideSystemUi", "", "initializePlayer", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onPause", "onResume", "onStart", "onStop", "onWindowFocusChanged", "hasFocus", "", "releasePlayer", "app_debug"})
public final class PlayerActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.example.stremiompvplayer.databinding.ActivityPlayerBinding binding;
    @org.jetbrains.annotations.Nullable()
    private com.example.stremiompvplayer.models.MetaItem currentMeta;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String currentStreamUrl;
    @org.jetbrains.annotations.Nullable()
    private com.example.stremiompvplayer.models.Stream nextEpisode;
    @org.jetbrains.annotations.Nullable()
    private com.example.stremiompvplayer.models.Stream prevEpisode;
    @org.jetbrains.annotations.Nullable()
    private androidx.media3.exoplayer.ExoPlayer player;
    @org.jetbrains.annotations.Nullable()
    private com.example.stremiompvplayer.models.Stream currentStream;
    private long currentPosition = 0L;
    
    public PlayerActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @androidx.annotation.OptIn(markerClass = {androidx.media3.common.util.UnstableApi.class})
    private final void initializePlayer() {
    }
    
    @java.lang.Override()
    public void onStart() {
    }
    
    @java.lang.Override()
    @androidx.annotation.OptIn(markerClass = {androidx.media3.common.util.UnstableApi.class})
    public void onResume() {
    }
    
    @java.lang.Override()
    @androidx.annotation.OptIn(markerClass = {androidx.media3.common.util.UnstableApi.class})
    public void onPause() {
    }
    
    @java.lang.Override()
    @androidx.annotation.OptIn(markerClass = {androidx.media3.common.util.UnstableApi.class})
    public void onStop() {
    }
    
    private final void releasePlayer() {
    }
    
    @java.lang.Override()
    public void onWindowFocusChanged(boolean hasFocus) {
    }
    
    private final void hideSystemUi() {
    }
}