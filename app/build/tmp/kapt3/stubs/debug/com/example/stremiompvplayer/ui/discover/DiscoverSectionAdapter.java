package com.example.stremiompvplayer.ui.discover;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0002\u0018\u00002\f\u0012\b\u0012\u00060\u0002R\u00020\u00000\u0001:\u0001\u0017B\u0019\u0012\u0012\u0010\u0003\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00060\u0004\u00a2\u0006\u0002\u0010\u0007J\b\u0010\u000b\u001a\u00020\fH\u0016J\u001c\u0010\r\u001a\u00020\u00062\n\u0010\u000e\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u000f\u001a\u00020\fH\u0016J\u001c\u0010\u0010\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\fH\u0016J\u0014\u0010\u0014\u001a\u00020\u00062\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\n0\u0016R\u001a\u0010\u0003\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00060\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lcom/example/stremiompvplayer/ui/discover/DiscoverSectionAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/example/stremiompvplayer/ui/discover/DiscoverSectionAdapter$ViewHolder;", "onItemClick", "Lkotlin/Function1;", "Lcom/example/stremiompvplayer/models/MetaPreview;", "", "(Lkotlin/jvm/functions/Function1;)V", "sections", "", "Lcom/example/stremiompvplayer/ui/discover/DiscoverSection;", "getItemCount", "", "onBindViewHolder", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "setSections", "newSections", "", "ViewHolder", "app_debug"})
public final class DiscoverSectionAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.example.stremiompvplayer.ui.discover.DiscoverSectionAdapter.ViewHolder> {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<com.example.stremiompvplayer.models.MetaPreview, kotlin.Unit> onItemClick = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.stremiompvplayer.ui.discover.DiscoverSection> sections = null;
    
    public DiscoverSectionAdapter(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.stremiompvplayer.models.MetaPreview, kotlin.Unit> onItemClick) {
        super();
    }
    
    public final void setSections(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.stremiompvplayer.ui.discover.DiscoverSection> newSections) {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.example.stremiompvplayer.ui.discover.DiscoverSectionAdapter.ViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.example.stremiompvplayer.ui.discover.DiscoverSectionAdapter.ViewHolder holder, int position) {
    }
    
    @java.lang.Override()
    public int getItemCount() {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fR\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/example/stremiompvplayer/ui/discover/DiscoverSectionAdapter$ViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "(Lcom/example/stremiompvplayer/ui/discover/DiscoverSectionAdapter;Landroid/view/View;)V", "sectionRecycler", "Landroidx/recyclerview/widget/RecyclerView;", "sectionTitle", "Landroid/widget/TextView;", "bind", "", "section", "Lcom/example/stremiompvplayer/ui/discover/DiscoverSection;", "app_debug"})
    public final class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView sectionTitle = null;
        @org.jetbrains.annotations.NotNull()
        private final androidx.recyclerview.widget.RecyclerView sectionRecycler = null;
        
        public ViewHolder(@org.jetbrains.annotations.NotNull()
        android.view.View itemView) {
            super(null);
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull()
        com.example.stremiompvplayer.ui.discover.DiscoverSection section) {
        }
    }
}