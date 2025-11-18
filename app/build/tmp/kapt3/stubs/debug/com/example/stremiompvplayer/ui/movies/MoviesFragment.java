package com.example.stremiompvplayer.ui.movies;

import android.content.Intent;
import android.os.Bundle;
import com.example.stremiompvplayer.adapters.CatalogChipAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stremiompvplayer.DetailsActivity2;
import com.example.stremiompvplayer.data.ServiceLocator;
import com.example.stremiompvplayer.databinding.FragmentMoviesBinding;
import com.example.stremiompvplayer.models.Catalog;
import com.example.stremiompvplayer.models.MetaItem;
import com.example.stremiompvplayer.models.FeedList;
import com.example.stremiompvplayer.ui.discover.DiscoverSectionAdapter;
import com.example.stremiompvplayer.viewmodels.CatalogViewModel;
import com.example.stremiompvplayer.viewmodels.CatalogUiState;
import android.util.Log;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0012\u001a\u00020\u0013H\u0002J$\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00172\b\u0010\u0018\u001a\u0004\u0018\u00010\u00192\b\u0010\u001a\u001a\u0004\u0018\u00010\u001bH\u0016J\b\u0010\u001c\u001a\u00020\u0013H\u0016J\u0010\u0010\u001d\u001a\u00020\u00132\u0006\u0010\u001e\u001a\u00020\u001fH\u0002J\u001a\u0010 \u001a\u00020\u00132\u0006\u0010!\u001a\u00020\u00152\b\u0010\u001a\u001a\u0004\u0018\u00010\u001bH\u0016J\u0016\u0010\"\u001a\u00020\u00132\f\u0010#\u001a\b\u0012\u0004\u0012\u00020%0$H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u000e\u0010\b\u001a\u00020\tX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082.\u00a2\u0006\u0002\n\u0000R\u001b\u0010\f\u001a\u00020\r8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0010\u0010\u0011\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006&"}, d2 = {"Lcom/example/stremiompvplayer/ui/movies/MoviesFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/example/stremiompvplayer/databinding/FragmentMoviesBinding;", "binding", "getBinding", "()Lcom/example/stremiompvplayer/databinding/FragmentMoviesBinding;", "catalogChipAdapter", "Lcom/example/stremiompvplayer/adapters/CatalogChipAdapter;", "contentAdapter", "Lcom/example/stremiompvplayer/ui/discover/DiscoverSectionAdapter;", "viewModel", "Lcom/example/stremiompvplayer/viewmodels/CatalogViewModel;", "getViewModel", "()Lcom/example/stremiompvplayer/viewmodels/CatalogViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "observeViewModel", "", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "savedInstanceState", "Landroid/os/Bundle;", "onDestroyView", "onPosterClick", "metaItem", "Lcom/example/stremiompvplayer/models/MetaItem;", "onViewCreated", "view", "setupCatalogChips", "movieCatalogs", "", "Lcom/example/stremiompvplayer/models/Catalog;", "app_debug"})
public final class MoviesFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.Nullable()
    private com.example.stremiompvplayer.databinding.FragmentMoviesBinding _binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private com.example.stremiompvplayer.ui.discover.DiscoverSectionAdapter contentAdapter;
    private com.example.stremiompvplayer.adapters.CatalogChipAdapter catalogChipAdapter;
    
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
    
    private final void observeViewModel() {
    }
    
    private final void setupCatalogChips(java.util.List<com.example.stremiompvplayer.models.Catalog> movieCatalogs) {
    }
    
    private final void onPosterClick(com.example.stremiompvplayer.models.MetaItem metaItem) {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
}