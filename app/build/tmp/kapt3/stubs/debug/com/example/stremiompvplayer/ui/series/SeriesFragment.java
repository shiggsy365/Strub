package com.example.stremiompvplayer.ui.series;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.stremiompvplayer.databinding.FragmentDiscoverBinding;
import com.example.stremiompvplayer.models.MetaItem;
import com.example.stremiompvplayer.DetailsActivity2;
import com.example.stremiompvplayer.ui.discover.DiscoverSectionAdapter;
import com.example.stremiompvplayer.viewmodels.MainViewModel;

/**
 * REWRITTEN: This fragment no longer fetches its own data.
 * It observes the ViewModel's `seriesSections` LiveData, which is
 * a filtered version of the main `discoverSections` list.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J$\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\b\u0010\u0012\u001a\u0004\u0018\u00010\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0015H\u0016J\b\u0010\u0016\u001a\u00020\u0017H\u0016J\u0010\u0010\u0018\u001a\u00020\u00172\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J\u001a\u0010\u001b\u001a\u00020\u00172\u0006\u0010\u001c\u001a\u00020\u000f2\b\u0010\u0014\u001a\u0004\u0018\u00010\u0015H\u0016R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\b\u001a\u00020\t8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\f\u0010\r\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u001d"}, d2 = {"Lcom/example/stremiompvplayer/ui/series/SeriesFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/example/stremiompvplayer/databinding/FragmentDiscoverBinding;", "binding", "getBinding", "()Lcom/example/stremiompvplayer/databinding/FragmentDiscoverBinding;", "viewModel", "Lcom/example/stremiompvplayer/viewmodels/MainViewModel;", "getViewModel", "()Lcom/example/stremiompvplayer/viewmodels/MainViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "savedInstanceState", "Landroid/os/Bundle;", "onDestroyView", "", "onPosterClick", "metaItem", "Lcom/example/stremiompvplayer/models/MetaItem;", "onViewCreated", "view", "app_debug"})
public final class SeriesFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.Nullable()
    private com.example.stremiompvplayer.databinding.FragmentDiscoverBinding _binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    
    public SeriesFragment() {
        super();
    }
    
    private final com.example.stremiompvplayer.databinding.FragmentDiscoverBinding getBinding() {
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
    
    private final void onPosterClick(com.example.stremiompvplayer.models.MetaItem metaItem) {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
}