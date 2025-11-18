package com.example.stremiompvplayer;

import android.net.Uri;
import android.os.Bundle;
import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.media.MediaPlayerAdapter;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.PlaybackControlsRow;

/**
 * Handles video playback with a customizable overlay using VideoSupportFragment.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0006\u001a\u00020\u00072\b\u0010\b\u001a\u0004\u0018\u00010\tH\u0016J\b\u0010\n\u001a\u00020\u0007H\u0016R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/example/stremiompvplayer/PlaybackVideoFragment;", "Landroidx/leanback/app/VideoSupportFragment;", "()V", "transportControlGlue", "Landroidx/leanback/media/PlaybackTransportControlGlue;", "Landroidx/leanback/media/MediaPlayerAdapter;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onPause", "app_debug"})
public final class PlaybackVideoFragment extends androidx.leanback.app.VideoSupportFragment {
    private androidx.leanback.media.PlaybackTransportControlGlue<androidx.leanback.media.MediaPlayerAdapter> transportControlGlue;
    
    public PlaybackVideoFragment() {
        super();
    }
    
    @java.lang.Override()
    public void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    public void onPause() {
    }
}