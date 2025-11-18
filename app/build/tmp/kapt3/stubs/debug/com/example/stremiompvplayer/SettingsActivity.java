package com.example.stremiompvplayer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.stremiompvplayer.adapters.AddonListAdapter;
import com.example.stremiompvplayer.databinding.ActivitySettingsBinding;
import com.example.stremiompvplayer.utils.SharedPreferencesManager;
import com.example.stremiompvplayer.utils.UserSettings;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\r\u001a\u00020\u000eH\u0002J\u0012\u0010\u000f\u001a\u00020\u000e2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0011H\u0014J\b\u0010\u0012\u001a\u00020\u000eH\u0002J\b\u0010\u0013\u001a\u00020\u000eH\u0002J\b\u0010\u0014\u001a\u00020\u000eH\u0002J\b\u0010\u0015\u001a\u00020\u000eH\u0002J\b\u0010\u0016\u001a\u00020\u000eH\u0002J\b\u0010\u0017\u001a\u00020\u000eH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lcom/example/stremiompvplayer/SettingsActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "addonAdapter", "Lcom/example/stremiompvplayer/adapters/AddonListAdapter;", "binding", "Lcom/example/stremiompvplayer/databinding/ActivitySettingsBinding;", "currentUserId", "", "prefsManager", "Lcom/example/stremiompvplayer/utils/SharedPreferencesManager;", "settings", "Lcom/example/stremiompvplayer/utils/UserSettings;", "loadSettings", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "saveSettings", "setupAddonsList", "setupListeners", "setupToolbar", "showAddAddonDialog", "updateSubtitlePreview", "app_debug"})
public final class SettingsActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.example.stremiompvplayer.utils.SharedPreferencesManager prefsManager;
    private com.example.stremiompvplayer.databinding.ActivitySettingsBinding binding;
    private com.example.stremiompvplayer.utils.UserSettings settings;
    private com.example.stremiompvplayer.adapters.AddonListAdapter addonAdapter;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String currentUserId;
    
    public SettingsActivity() {
        super();
    }
    
    private final void setupAddonsList() {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupToolbar() {
    }
    
    private final void loadSettings() {
    }
    
    private final void setupListeners() {
    }
    
    private final void updateSubtitlePreview() {
    }
    
    private final void saveSettings() {
    }
    
    private final void showAddAddonDialog() {
    }
}