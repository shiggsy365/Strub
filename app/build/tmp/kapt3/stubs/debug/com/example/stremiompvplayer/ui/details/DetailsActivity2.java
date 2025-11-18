package com.example.stremiompvplayer.ui.details;

/**
 * REFACTORED: This Activity no longer fetches its own data.
 * It observes the MainViewModel, which ensures data is loaded
 * using the correct user's authKey.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0016H\u0014J\b\u0010\u0017\u001a\u00020\u0014H\u0014J\u0010\u0010\u0018\u001a\u00020\u00142\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J\b\u0010\u001b\u001a\u00020\u0014H\u0002J\b\u0010\u001c\u001a\u00020\u0014H\u0002J\u0010\u0010\u001d\u001a\u00020\u00142\u0006\u0010\u001e\u001a\u00020\u0007H\u0002R\u0010\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0004\n\u0002\u0010\u0005R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\b\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\n\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u001b\u0010\r\u001a\u00020\u000e8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0011\u0010\u0012\u001a\u0004\b\u000f\u0010\u0010\u00a8\u0006\u001f"}, d2 = {"Lcom/example/stremiompvplayer/ui/details/DetailsActivity2;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "error/NonExistentClass", "Lerror/NonExistentClass;", "currentMeta", "Lcom/example/stremiompvplayer/models/Meta;", "metaId", "", "metaType", "streamAdapter", "Lcom/example/stremiompvplayer/adapters/StreamAdapter;", "viewModel", "Lcom/example/stremiompvplayer/viewmodels/MainViewModel;", "getViewModel", "()Lcom/example/stremiompvplayer/viewmodels/MainViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "onStreamClick", "stream", "Lcom/example/stremiompvplayer/models/Stream;", "setupObservers", "setupRecyclerView", "updateUI", "meta", "app_debug"})
public final class DetailsActivity2 extends androidx.appcompat.app.AppCompatActivity {
    private error.NonExistentClass binding;
    private com.example.stremiompvplayer.adapters.StreamAdapter streamAdapter;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String metaId;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String metaType;
    @org.jetbrains.annotations.Nullable()
    private com.example.stremiompvplayer.models.Meta currentMeta;
    
    public DetailsActivity2() {
        super();
    }
    
    private final com.example.stremiompvplayer.viewmodels.MainViewModel getViewModel() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupObservers() {
    }
    
    private final void setupRecyclerView() {
    }
    
    private final void updateUI(com.example.stremiompvplayer.models.Meta meta) {
    }
    
    private final void onStreamClick(com.example.stremiompvplayer.models.Stream stream) {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}