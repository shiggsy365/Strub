package com.example.stremiompvplayer;

/**
 * BrowseErrorActivity shows how to use ErrorFragment.
 * NOTE: This is from the Android TV Leanback template - just a demo.
 * You probably don't need this unless you're specifically testing error screens.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 \f2\u00020\u0001:\u0002\f\rB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0007\u001a\u00020\b2\b\u0010\t\u001a\u0004\u0018\u00010\nH\u0014J\b\u0010\u000b\u001a\u00020\bH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lcom/example/stremiompvplayer/BrowseErrorActivity;", "Landroidx/fragment/app/FragmentActivity;", "()V", "mErrorFragment", "Lcom/example/stremiompvplayer/ErrorFragment;", "mSpinnerFragment", "Lcom/example/stremiompvplayer/BrowseErrorActivity$SpinnerFragment;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "testError", "Companion", "SpinnerFragment", "app_debug"})
public final class BrowseErrorActivity extends androidx.fragment.app.FragmentActivity {
    private com.example.stremiompvplayer.ErrorFragment mErrorFragment;
    private com.example.stremiompvplayer.BrowseErrorActivity.SpinnerFragment mSpinnerFragment;
    private static final long TIMER_DELAY = 3000L;
    private static final int SPINNER_WIDTH = 100;
    private static final int SPINNER_HEIGHT = 100;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.stremiompvplayer.BrowseErrorActivity.Companion Companion = null;
    
    public BrowseErrorActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void testError() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/example/stremiompvplayer/BrowseErrorActivity$Companion;", "", "()V", "SPINNER_HEIGHT", "", "SPINNER_WIDTH", "TIMER_DELAY", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J&\u0010\u0003\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0005\u001a\u00020\u00062\b\u0010\u0007\u001a\u0004\u0018\u00010\b2\b\u0010\t\u001a\u0004\u0018\u00010\nH\u0016\u00a8\u0006\u000b"}, d2 = {"Lcom/example/stremiompvplayer/BrowseErrorActivity$SpinnerFragment;", "Landroidx/fragment/app/Fragment;", "()V", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "savedInstanceState", "Landroid/os/Bundle;", "app_debug"})
    public static final class SpinnerFragment extends androidx.fragment.app.Fragment {
        
        public SpinnerFragment() {
            super();
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.Nullable()
        public android.view.View onCreateView(@org.jetbrains.annotations.NotNull()
        android.view.LayoutInflater inflater, @org.jetbrains.annotations.Nullable()
        android.view.ViewGroup container, @org.jetbrains.annotations.Nullable()
        android.os.Bundle savedInstanceState) {
            return null;
        }
    }
}