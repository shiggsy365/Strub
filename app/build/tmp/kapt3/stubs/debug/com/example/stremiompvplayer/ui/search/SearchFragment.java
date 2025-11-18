package com.example.stremiompvplayer.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.stremiompvplayer.adapters.ContentAdapter;
import com.example.stremiompvplayer.databinding.FragmentSearchBinding;
import com.example.stremiompvplayer.models.Manifest;
import com.example.stremiompvplayer.models.MetaItem;
import com.example.stremiompvplayer.network.StremioClient;
import com.example.stremiompvplayer.DetailsActivity2;
import com.example.stremiompvplayer.viewmodels.MainViewModel;
import kotlinx.coroutines.Dispatchers;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J$\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u0016J\b\u0010\u0018\u001a\u00020\u0019H\u0016J\u0010\u0010\u001a\u001a\u00020\u00192\u0006\u0010\u001b\u001a\u00020\u001cH\u0002J\u001a\u0010\u001d\u001a\u00020\u00192\u0006\u0010\u001e\u001a\u00020\u00112\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u0016J\u0010\u0010\u001f\u001a\u00020\u00192\u0006\u0010 \u001a\u00020!H\u0002J\u0010\u0010\"\u001a\u00020\u00192\u0006\u0010 \u001a\u00020!H\u0002J\b\u0010#\u001a\u00020\u0019H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u000e\u0010\b\u001a\u00020\tX\u0082.\u00a2\u0006\u0002\n\u0000R\u001b\u0010\n\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\r\u00a8\u0006$"}, d2 = {"Lcom/example/stremiompvplayer/ui/search/SearchFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/example/stremiompvplayer/databinding/FragmentSearchBinding;", "binding", "getBinding", "()Lcom/example/stremiompvplayer/databinding/FragmentSearchBinding;", "searchAdapter", "Lcom/example/stremiompvplayer/adapters/ContentAdapter;", "viewModel", "Lcom/example/stremiompvplayer/viewmodels/MainViewModel;", "getViewModel", "()Lcom/example/stremiompvplayer/viewmodels/MainViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "savedInstanceState", "Landroid/os/Bundle;", "onDestroyView", "", "onPosterClick", "metaItem", "Lcom/example/stremiompvplayer/models/MetaItem;", "onViewCreated", "view", "searchAddon", "query", "", "searchStremio", "setupRecyclerView", "app_debug"})
public final class SearchFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.Nullable()
    private com.example.stremiompvplayer.databinding.FragmentSearchBinding _binding;
    private com.example.stremiompvplayer.adapters.ContentAdapter searchAdapter;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    
    public SearchFragment() {
        super();
    }
    
    private final com.example.stremiompvplayer.databinding.FragmentSearchBinding getBinding() {
        return null;
    }
    
    private final com.example.stremiompvplayer.viewmodels.MainViewModel getViewModel() {
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
    
    private final void setupRecyclerView() {
    }
    
    private final void searchStremio(java.lang.String query) {
    }
    
    private final void searchAddon(java.lang.String query) {
    }
    
    private final void onPosterClick(com.example.stremiompvplayer.models.MetaItem metaItem) {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
}