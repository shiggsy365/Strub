package com.example.stremiompvplayer;

/**
 * Loads a grid of cards with movies to browse.
 * NOTE: This is from the Android TV Leanback template - just a demo.
 * You may not need this if you're using MainActivity with tab navigation.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\r\u0018\u0000 \u001c2\u00020\u0001:\u0005\u001c\u001d\u001e\u001f B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0010\u001a\u00020\u0011H\u0002J\u0012\u0010\u0012\u001a\u00020\u00112\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0016J\b\u0010\u0015\u001a\u00020\u0011H\u0016J\b\u0010\u0016\u001a\u00020\u0011H\u0002J\b\u0010\u0017\u001a\u00020\u0011H\u0002J\b\u0010\u0018\u001a\u00020\u0011H\u0002J\b\u0010\u0019\u001a\u00020\u0011H\u0002J\u0012\u0010\u001a\u001a\u00020\u00112\b\u0010\u001b\u001a\u0004\u0018\u00010\tH\u0002R\u0010\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0004\n\u0002\u0010\u0005R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\b\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2 = {"Lcom/example/stremiompvplayer/MainFragment;", "", "()V", "mBackgroundManager", "error/NonExistentClass", "Lerror/NonExistentClass;", "mBackgroundTimer", "Ljava/util/Timer;", "mBackgroundUri", "", "mDefaultBackground", "Landroid/graphics/drawable/Drawable;", "mHandler", "Landroid/os/Handler;", "mMetrics", "Landroid/util/DisplayMetrics;", "loadRows", "", "onActivityCreated", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "prepareBackgroundManager", "setupEventListeners", "setupUIElements", "startBackgroundTimer", "updateBackground", "uri", "Companion", "GridItemPresenter", "ItemViewClickedListener", "ItemViewSelectedListener", "UpdateBackgroundTask", "app_debug"})
public final class MainFragment {
    @org.jetbrains.annotations.NotNull()
    private final android.os.Handler mHandler = null;
    private error.NonExistentClass mBackgroundManager;
    @org.jetbrains.annotations.Nullable()
    private android.graphics.drawable.Drawable mDefaultBackground;
    private android.util.DisplayMetrics mMetrics;
    @org.jetbrains.annotations.Nullable()
    private java.util.Timer mBackgroundTimer;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String mBackgroundUri;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "MainFragment";
    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    private static final int NUM_ROWS = 6;
    private static final int NUM_COLS = 15;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.stremiompvplayer.MainFragment.Companion Companion = null;
    
    public MainFragment() {
        super();
    }
    
    public void onActivityCreated(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    public void onDestroy() {
    }
    
    private final void prepareBackgroundManager() {
    }
    
    private final void setupUIElements() {
    }
    
    private final void loadRows() {
    }
    
    private final void setupEventListeners() {
    }
    
    private final void updateBackground(java.lang.String uri) {
    }
    
    private final void startBackgroundTimer() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/example/stremiompvplayer/MainFragment$Companion;", "", "()V", "BACKGROUND_UPDATE_DELAY", "", "GRID_ITEM_HEIGHT", "GRID_ITEM_WIDTH", "NUM_COLS", "NUM_ROWS", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0082\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u001d\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u0001H\u0016\u00a2\u0006\u0002\u0010\bJ\u0015\u0010\t\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u000bH\u0016\u00a2\u0006\u0002\u0010\fJ\u0015\u0010\r\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0016\u00a2\u0006\u0002\u0010\u000e\u00a8\u0006\u000f"}, d2 = {"Lcom/example/stremiompvplayer/MainFragment$GridItemPresenter;", "", "(Lcom/example/stremiompvplayer/MainFragment;)V", "onBindViewHolder", "", "viewHolder", "error/NonExistentClass", "item", "(Lerror/NonExistentClass;Ljava/lang/Object;)V", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "(Landroid/view/ViewGroup;)Lerror/NonExistentClass;", "onUnbindViewHolder", "(Lerror/NonExistentClass;)V", "app_debug"})
    final class GridItemPresenter {
        
        public GridItemPresenter() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public error.NonExistentClass onCreateViewHolder(@org.jetbrains.annotations.NotNull()
        android.view.ViewGroup parent) {
            return null;
        }
        
        public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
        error.NonExistentClass viewHolder, @org.jetbrains.annotations.NotNull()
        java.lang.Object item) {
        }
        
        public void onUnbindViewHolder(@org.jetbrains.annotations.NotNull()
        error.NonExistentClass viewHolder) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0007\b\u0082\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J-\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\u0006H\u0016\u00a2\u0006\u0002\u0010\n\u00a8\u0006\u000b"}, d2 = {"Lcom/example/stremiompvplayer/MainFragment$ItemViewClickedListener;", "", "(Lcom/example/stremiompvplayer/MainFragment;)V", "onItemClicked", "", "itemViewHolder", "error/NonExistentClass", "item", "rowViewHolder", "row", "(Lerror/NonExistentClass;Ljava/lang/Object;Lerror/NonExistentClass;Lerror/NonExistentClass;)V", "app_debug"})
    final class ItemViewClickedListener {
        
        public ItemViewClickedListener() {
            super();
        }
        
        public void onItemClicked(@org.jetbrains.annotations.NotNull()
        error.NonExistentClass itemViewHolder, @org.jetbrains.annotations.NotNull()
        java.lang.Object item, @org.jetbrains.annotations.NotNull()
        error.NonExistentClass rowViewHolder, @org.jetbrains.annotations.NotNull()
        error.NonExistentClass row) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0007\b\u0082\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J/\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\b\u0010\u0007\u001a\u0004\u0018\u00010\u00012\u0006\u0010\b\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\u0006H\u0016\u00a2\u0006\u0002\u0010\n\u00a8\u0006\u000b"}, d2 = {"Lcom/example/stremiompvplayer/MainFragment$ItemViewSelectedListener;", "", "(Lcom/example/stremiompvplayer/MainFragment;)V", "onItemSelected", "", "itemViewHolder", "error/NonExistentClass", "item", "rowViewHolder", "row", "(Lerror/NonExistentClass;Ljava/lang/Object;Lerror/NonExistentClass;Lerror/NonExistentClass;)V", "app_debug"})
    final class ItemViewSelectedListener {
        
        public ItemViewSelectedListener() {
            super();
        }
        
        public void onItemSelected(@org.jetbrains.annotations.Nullable()
        error.NonExistentClass itemViewHolder, @org.jetbrains.annotations.Nullable()
        java.lang.Object item, @org.jetbrains.annotations.NotNull()
        error.NonExistentClass rowViewHolder, @org.jetbrains.annotations.NotNull()
        error.NonExistentClass row) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\b\u0082\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0016\u00a8\u0006\u0005"}, d2 = {"Lcom/example/stremiompvplayer/MainFragment$UpdateBackgroundTask;", "Ljava/util/TimerTask;", "(Lcom/example/stremiompvplayer/MainFragment;)V", "run", "", "app_debug"})
    final class UpdateBackgroundTask extends java.util.TimerTask {
        
        public UpdateBackgroundTask() {
            super();
        }
        
        @java.lang.Override()
        public void run() {
        }
    }
}