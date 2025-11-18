package com.example.stremiompvplayer.ui.movies;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.stremiompvplayer.PlayerActivity;
import com.example.stremiompvplayer.data.AppDatabase;
import com.example.stremiompvplayer.data.ServiceLocator;
import com.example.stremiompvplayer.databinding.FragmentMoviesBinding;
import com.example.stremiompvplayer.models.Catalog;
import com.example.stremiompvplayer.models.CollectedItem;
import com.example.stremiompvplayer.models.MetaItem;
import com.example.stremiompvplayer.models.Stream;
import com.example.stremiompvplayer.models.UserCatalog;
import com.example.stremiompvplayer.adapters.CatalogChipAdapter;
import com.example.stremiompvplayer.adapters.StreamAdapter;
import com.example.stremiompvplayer.adapters.PosterAdapter;
import com.example.stremiompvplayer.utils.SharedPreferencesManager;
import com.example.stremiompvplayer.viewmodels.CatalogViewModel;
import com.example.stremiompvplayer.viewmodels.CatalogUiState;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u008a\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0018\u0002\n\u0002\b\t\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u001f\u001a\u00020 2\u0006\u0010!\u001a\u00020\u000bH\u0002J\u0010\u0010\"\u001a\u00020 2\u0006\u0010!\u001a\u00020\u000bH\u0002J\u0016\u0010#\u001a\u00020 2\f\u0010$\u001a\b\u0012\u0004\u0012\u00020%0\u0017H\u0002J\u0010\u0010&\u001a\u00020 2\u0006\u0010!\u001a\u00020\u000bH\u0002J\b\u0010\'\u001a\u00020 H\u0002J \u0010(\u001a\u00020 2\u0006\u0010)\u001a\u00020\u00182\u0006\u0010*\u001a\u00020+2\u0006\u0010,\u001a\u00020+H\u0002J\b\u0010-\u001a\u00020 H\u0002J\b\u0010.\u001a\u00020 H\u0002J$\u0010/\u001a\u0002002\u0006\u00101\u001a\u0002022\b\u00103\u001a\u0004\u0018\u0001042\b\u00105\u001a\u0004\u0018\u000106H\u0016J\b\u00107\u001a\u00020 H\u0016J\u0010\u00108\u001a\u00020 2\u0006\u00109\u001a\u00020%H\u0002J\u001a\u0010:\u001a\u00020 2\u0006\u0010;\u001a\u0002002\b\u00105\u001a\u0004\u0018\u000106H\u0016J\u0010\u0010<\u001a\u00020 2\u0006\u0010)\u001a\u00020\u0018H\u0002J\u0018\u0010=\u001a\u00020 2\u0006\u0010)\u001a\u00020\u00182\u0006\u0010>\u001a\u00020\rH\u0002J\b\u0010?\u001a\u00020 H\u0002J\b\u0010@\u001a\u00020 H\u0002J\b\u0010A\u001a\u00020 H\u0002J\u0010\u0010B\u001a\u00020 2\u0006\u0010)\u001a\u00020CH\u0002J\u0010\u0010D\u001a\u00020 2\u0006\u0010!\u001a\u00020\u000bH\u0002J\b\u0010E\u001a\u00020 H\u0002J\u0010\u0010F\u001a\u00020 2\u0006\u0010G\u001a\u00020\u0018H\u0002J\u0010\u0010H\u001a\u00020 2\u0006\u0010G\u001a\u00020\u0018H\u0002J\u0010\u0010I\u001a\u00020 2\u0006\u0010G\u001a\u00020\u0018H\u0002J\u0010\u0010J\u001a\u00020 2\u0006\u0010K\u001a\u00020\rH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u000e\u0010\b\u001a\u00020\tX\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0015X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00180\u0017X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0019\u001a\u00020\u001a8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001d\u0010\u001e\u001a\u0004\b\u001b\u0010\u001c\u00a8\u0006L"}, d2 = {"Lcom/example/stremiompvplayer/ui/movies/MoviesFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/example/stremiompvplayer/databinding/FragmentMoviesBinding;", "binding", "getBinding", "()Lcom/example/stremiompvplayer/databinding/FragmentMoviesBinding;", "catalogChipAdapter", "Lcom/example/stremiompvplayer/adapters/CatalogChipAdapter;", "currentMetaItem", "Lcom/example/stremiompvplayer/models/MetaItem;", "currentUserId", "", "db", "Lcom/example/stremiompvplayer/data/AppDatabase;", "posterAdapter", "Lcom/example/stremiompvplayer/adapters/PosterAdapter;", "prefsManager", "Lcom/example/stremiompvplayer/utils/SharedPreferencesManager;", "streamAdapter", "Lcom/example/stremiompvplayer/adapters/StreamAdapter;", "userCatalogs", "", "Lcom/example/stremiompvplayer/models/UserCatalog;", "viewModel", "Lcom/example/stremiompvplayer/viewmodels/CatalogViewModel;", "getViewModel", "()Lcom/example/stremiompvplayer/viewmodels/CatalogViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "collectItem", "", "metaItem", "displayMovieDetails", "displayStreams", "streams", "Lcom/example/stremiompvplayer/models/Stream;", "fetchStreamsForMovie", "hideEmptyState", "moveCatalog", "catalog", "fromPosition", "", "toPosition", "observeUserCatalogs", "observeViewModel", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "savedInstanceState", "Landroid/os/Bundle;", "onDestroyView", "onStreamClick", "stream", "onViewCreated", "view", "removeCatalog", "renameCatalog", "newName", "setupCatalogChips", "setupMovieGrid", "setupStreamsRecycler", "showCatalogOptionsDialog", "Lcom/example/stremiompvplayer/models/Catalog;", "showCollectionDialog", "showEmptyState", "showMoveCatalogDialog", "userCatalog", "showRemoveCatalogDialog", "showRenameCatalogDialog", "uncollectItem", "collectedId", "app_debug"})
public final class MoviesFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.Nullable()
    private com.example.stremiompvplayer.databinding.FragmentMoviesBinding _binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private com.example.stremiompvplayer.adapters.CatalogChipAdapter catalogChipAdapter;
    private com.example.stremiompvplayer.adapters.StreamAdapter streamAdapter;
    private com.example.stremiompvplayer.adapters.PosterAdapter posterAdapter;
    private com.example.stremiompvplayer.utils.SharedPreferencesManager prefsManager;
    private com.example.stremiompvplayer.data.AppDatabase db;
    @org.jetbrains.annotations.Nullable()
    private com.example.stremiompvplayer.models.MetaItem currentMetaItem;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String currentUserId;
    @org.jetbrains.annotations.NotNull()
    private java.util.List<com.example.stremiompvplayer.models.UserCatalog> userCatalogs;
    
    public MoviesFragment() {
        super();
    }
    
    private final com.example.stremiompvplayer.databinding.FragmentMoviesBinding getBinding() {
        return null;
    }
    
    private final com.example.stremiompvplayer.viewmodels.CatalogViewModel getViewModel() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public android.view.View onCreateView(@org.jetbrains.annotations.NotNull()
    android.view.LayoutInflater inflater, @org.jetbrains.annotations.Nullable()
    android.view.ViewGroup container, @org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
        return null;
    }
    
    @java.lang.Override()
    public void onViewCreated(@org.jetbrains.annotations.NotNull()
    android.view.View view, @org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupCatalogChips() {
    }
    
    private final void setupStreamsRecycler() {
    }
    
    private final void setupMovieGrid() {
    }
    
    private final void observeUserCatalogs() {
    }
    
    private final void observeViewModel() {
    }
    
    private final void showEmptyState() {
    }
    
    private final void hideEmptyState() {
    }
    
    private final void showCatalogOptionsDialog(com.example.stremiompvplayer.models.Catalog catalog) {
    }
    
    private final void showMoveCatalogDialog(com.example.stremiompvplayer.models.UserCatalog userCatalog) {
    }
    
    private final void moveCatalog(com.example.stremiompvplayer.models.UserCatalog catalog, int fromPosition, int toPosition) {
    }
    
    private final void showRenameCatalogDialog(com.example.stremiompvplayer.models.UserCatalog userCatalog) {
    }
    
    private final void renameCatalog(com.example.stremiompvplayer.models.UserCatalog catalog, java.lang.String newName) {
    }
    
    private final void showRemoveCatalogDialog(com.example.stremiompvplayer.models.UserCatalog userCatalog) {
    }
    
    private final void removeCatalog(com.example.stremiompvplayer.models.UserCatalog catalog) {
    }
    
    private final void showCollectionDialog(com.example.stremiompvplayer.models.MetaItem metaItem) {
    }
    
    private final void collectItem(com.example.stremiompvplayer.models.MetaItem metaItem) {
    }
    
    private final void uncollectItem(java.lang.String collectedId) {
    }
    
    private final void displayMovieDetails(com.example.stremiompvplayer.models.MetaItem metaItem) {
    }
    
    private final void fetchStreamsForMovie(com.example.stremiompvplayer.models.MetaItem metaItem) {
    }
    
    private final void displayStreams(java.util.List<com.example.stremiompvplayer.models.Stream> streams) {
    }
    
    private final void onStreamClick(com.example.stremiompvplayer.models.Stream stream) {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
}