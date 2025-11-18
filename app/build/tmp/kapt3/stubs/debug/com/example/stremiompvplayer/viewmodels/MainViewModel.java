package com.example.stremiompvplayer.viewmodels;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0007\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u001e\u001a\u00020\u001fJ\u0006\u0010 \u001a\u00020\u001fJ\u0006\u0010!\u001a\u00020\u001fJ\u0016\u0010\u0011\u001a\u00020\u001f2\u0006\u0010\"\u001a\u00020\u00062\u0006\u0010#\u001a\u00020\u0006J\u0016\u0010\u001a\u001a\u00020\u001f2\u0006\u0010\"\u001a\u00020\u00062\u0006\u0010#\u001a\u00020\u0006J\u000e\u0010$\u001a\u00020\u001f2\u0006\u0010%\u001a\u00020\u0006R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u001d\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\fR\u0019\u0010\u000f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00100\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\fR\u001d\u0010\u0012\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u001d\u0010\u0016\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0015R\u001d\u0010\u0018\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00190\t0\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\fR\u000e\u0010\u001b\u001a\u00020\u001cX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006&"}, d2 = {"Lcom/example/stremiompvplayer/viewmodels/MainViewModel;", "Landroidx/lifecycle/AndroidViewModel;", "application", "Landroid/app/Application;", "(Landroid/app/Application;)V", "currentUserAuthKey", "", "discoverSections", "Landroidx/lifecycle/MutableLiveData;", "", "Lcom/example/stremiompvplayer/models/FeedList;", "getDiscoverSections", "()Landroidx/lifecycle/MutableLiveData;", "feedList", "getFeedList", "meta", "Lcom/example/stremiompvplayer/models/Meta;", "getMeta", "movieSections", "Landroidx/lifecycle/LiveData;", "getMovieSections", "()Landroidx/lifecycle/LiveData;", "seriesSections", "getSeriesSections", "streams", "Lcom/example/stremiompvplayer/models/Stream;", "getStreams", "stremioClient", "Lcom/example/stremiompvplayer/network/StremioClient;", "userManifestsPrefsName", "clearMetaAndStreams", "", "fetchCatalogs", "getFeed", "type", "id", "setCurrentUser", "authKey", "app_debug"})
public final class MainViewModel extends androidx.lifecycle.AndroidViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.example.stremiompvplayer.network.StremioClient stremioClient = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String currentUserAuthKey;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String userManifestsPrefsName = "manifests_default";
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.MutableLiveData<java.util.List<com.example.stremiompvplayer.models.FeedList>> feedList = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.MutableLiveData<com.example.stremiompvplayer.models.Meta> meta = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.MutableLiveData<java.util.List<com.example.stremiompvplayer.models.Stream>> streams = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.MutableLiveData<java.util.List<com.example.stremiompvplayer.models.FeedList>> discoverSections = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.LiveData<java.util.List<com.example.stremiompvplayer.models.FeedList>> movieSections = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.LiveData<java.util.List<com.example.stremiompvplayer.models.FeedList>> seriesSections = null;
    
    public MainViewModel(@org.jetbrains.annotations.NotNull()
    android.app.Application application) {
        super(null);
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.MutableLiveData<java.util.List<com.example.stremiompvplayer.models.FeedList>> getFeedList() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.MutableLiveData<com.example.stremiompvplayer.models.Meta> getMeta() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.MutableLiveData<java.util.List<com.example.stremiompvplayer.models.Stream>> getStreams() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.MutableLiveData<java.util.List<com.example.stremiompvplayer.models.FeedList>> getDiscoverSections() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<java.util.List<com.example.stremiompvplayer.models.FeedList>> getMovieSections() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<java.util.List<com.example.stremiompvplayer.models.FeedList>> getSeriesSections() {
        return null;
    }
    
    /**
     * NEW: Call this from your Activity (e.g., MainActivity or UserSelectionActivity)
     * when the user is selected or logs in.
     */
    public final void setCurrentUser(@org.jetbrains.annotations.NotNull()
    java.lang.String authKey) {
    }
    
    public final void getFeed() {
    }
    
    /**
     * NEW: Fetches catalogs directly from an add-on, based on index.html logic.
     * This will populate the `discoverSections` LiveData.
     */
    public final void fetchCatalogs() {
    }
    
    public final void getMeta(@org.jetbrains.annotations.NotNull()
    java.lang.String type, @org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    public final void getStreams(@org.jetbrains.annotations.NotNull()
    java.lang.String type, @org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    public final void clearMetaAndStreams() {
    }
}