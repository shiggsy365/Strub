package com.example.stremiompvplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.stremiompvplayer.databinding.ActivityMainBinding;
import com.example.stremiompvplayer.ui.discover.DiscoverFragment;
import com.example.stremiompvplayer.ui.library.LibraryFragment;
import com.example.stremiompvplayer.ui.movies.MoviesFragment;
import com.example.stremiompvplayer.ui.search.SearchFragment;
import com.example.stremiompvplayer.ui.series.SeriesFragment;
import com.example.stremiompvplayer.utils.SharedPreferencesManager;
import com.example.stremiompvplayer.utils.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.example.stremiompvplayer.UserSelectionActivity;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0002J\u0012\u0010\r\u001a\u00020\n2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u0014J\b\u0010\u0010\u001a\u00020\nH\u0002J\b\u0010\u0011\u001a\u00020\nH\u0002J\b\u0010\u0012\u001a\u00020\nH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/example/stremiompvplayer/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/example/stremiompvplayer/databinding/ActivityMainBinding;", "currentUserId", "", "prefsManager", "Lcom/example/stremiompvplayer/utils/SharedPreferencesManager;", "loadFragment", "", "fragment", "Landroidx/fragment/app/Fragment;", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "setupNavigation", "setupUserAvatar", "showUserMenu", "app_debug"})
public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.example.stremiompvplayer.databinding.ActivityMainBinding binding;
    private com.example.stremiompvplayer.utils.SharedPreferencesManager prefsManager;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String currentUserId;
    
    public MainActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupUserAvatar() {
    }
    
    private final void showUserMenu() {
    }
    
    private final void setupNavigation() {
    }
    
    private final void loadFragment(androidx.fragment.app.Fragment fragment) {
    }
}