package com.example.stremiompvplayer;

/**
 * Loads a grid of cards with movies to browse.
 * NOTE: This is from the Android TV Leanback template - just a demo.
 * You may not need this if you're using MainActivity with tab navigation.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\r\u0018\u0000 \u001b2\u00020\u0001:\u0005\u001b\u001c\u001d\u001e\u001fB\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u000f\u001a\u00020\u0010H\u0002J\u0012\u0010\u0011\u001a\u00020\u00102\b\u0010\u0012\u001a\u0004\u0018\u00010\u0013H\u0016J\b\u0010\u0014\u001a\u00020\u0010H\u0016J\b\u0010\u0015\u001a\u00020\u0010H\u0002J\b\u0010\u0016\u001a\u00020\u0010H\u0002J\b\u0010\u0017\u001a\u00020\u0010H\u0002J\b\u0010\u0018\u001a\u00020\u0010H\u0002J\u0012\u0010\u0019\u001a\u00020\u00102\b\u0010\u001a\u001a\u0004\u0018\u00010\bH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\t\u001a\u0004\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Lcom/example/stremiompvplayer/MainFragment;", "Landroidx/leanback/app/BrowseSupportFragment;", "()V", "mBackgroundManager", "Landroidx/leanback/app/BackgroundManager;", "mBackgroundTimer", "Ljava/util/Timer;", "mBackgroundUri", "", "mDefaultBackground", "Landroid/graphics/drawable/Drawable;", "mHandler", "Landroid/os/Handler;", "mMetrics", "Landroid/util/DisplayMetrics;", "loadRows", "", "onActivityCreated", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "prepareBackgroundManager", "setupEventListeners", "setupUIElements", "startBackgroundTimer", "updateBackground", "uri", "Companion", "GridItemPresenter", "ItemViewClickedListener", "ItemViewSelectedListener", "UpdateBackgroundTask", "app_debug"})
public final class MainFragment extends androidx.leanback.app.BrowseSupportFragment {
    @org.jetbrains.annotations.NotNull()
    private final android.os.Handler mHandler = null;
    private androidx.leanback.app.BackgroundManager mBackgroundManager;
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
    
    @java.lang.Override()
    public void onActivityCreated(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0082\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0016J\u0010\u0010\t\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u000bH\u0016J\u0010\u0010\f\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0016\u00a8\u0006\r"}, d2 = {"Lcom/example/stremiompvplayer/MainFragment$GridItemPresenter;", "Landroidx/leanback/widget/Presenter;", "(Lcom/example/stremiompvplayer/MainFragment;)V", "onBindViewHolder", "", "viewHolder", "Landroidx/leanback/widget/Presenter$ViewHolder;", "item", "", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "onUnbindViewHolder", "app_debug"})
    final class GridItemPresenter extends androidx.leanback.widget.Presenter {
        
        public GridItemPresenter() {
            super();
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public androidx.leanback.widget.Presenter.ViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
        android.view.ViewGroup parent) {
            return null;
        }
        
        @java.lang.Override()
        public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
        androidx.leanback.widget.Presenter.ViewHolder viewHolder, @org.jetbrains.annotations.NotNull()
        java.lang.Object item) {
        }
        
        @java.lang.Override()
        public void onUnbindViewHolder(@org.jetbrains.annotations.NotNull()
        androidx.leanback.widget.Presenter.ViewHolder viewHolder) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0082\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J(\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0016\u00a8\u0006\r"}, d2 = {"Lcom/example/stremiompvplayer/MainFragment$ItemViewClickedListener;", "Landroidx/leanback/widget/OnItemViewClickedListener;", "(Lcom/example/stremiompvplayer/MainFragment;)V", "onItemClicked", "", "itemViewHolder", "Landroidx/leanback/widget/Presenter$ViewHolder;", "item", "", "rowViewHolder", "Landroidx/leanback/widget/RowPresenter$ViewHolder;", "row", "Landroidx/leanback/widget/Row;", "app_debug"})
    final class ItemViewClickedListener implements androidx.leanback.widget.OnItemViewClickedListener {
        
        public ItemViewClickedListener() {
            super();
        }
        
        @java.lang.Override()
        public void onItemClicked(@org.jetbrains.annotations.NotNull()
        androidx.leanback.widget.Presenter.ViewHolder itemViewHolder, @org.jetbrains.annotations.NotNull()
        java.lang.Object item, @org.jetbrains.annotations.NotNull()
        androidx.leanback.widget.RowPresenter.ViewHolder rowViewHolder, @org.jetbrains.annotations.NotNull()
        androidx.leanback.widget.Row row) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0082\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J,\u0010\u0003\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u00062\b\u0010\u0007\u001a\u0004\u0018\u00010\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0016\u00a8\u0006\r"}, d2 = {"Lcom/example/stremiompvplayer/MainFragment$ItemViewSelectedListener;", "Landroidx/leanback/widget/OnItemViewSelectedListener;", "(Lcom/example/stremiompvplayer/MainFragment;)V", "onItemSelected", "", "itemViewHolder", "Landroidx/leanback/widget/Presenter$ViewHolder;", "item", "", "rowViewHolder", "Landroidx/leanback/widget/RowPresenter$ViewHolder;", "row", "Landroidx/leanback/widget/Row;", "app_debug"})
    final class ItemViewSelectedListener implements androidx.leanback.widget.OnItemViewSelectedListener {
        
        public ItemViewSelectedListener() {
            super();
        }
        
        @java.lang.Override()
        public void onItemSelected(@org.jetbrains.annotations.Nullable()
        androidx.leanback.widget.Presenter.ViewHolder itemViewHolder, @org.jetbrains.annotations.Nullable()
        java.lang.Object item, @org.jetbrains.annotations.NotNull()
        androidx.leanback.widget.RowPresenter.ViewHolder rowViewHolder, @org.jetbrains.annotations.NotNull()
        androidx.leanback.widget.Row row) {
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