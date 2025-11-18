package com.example.stremiompvplayer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stremiompvplayer.R;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001\u0017B\'\u0012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u0012\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\u0002\u0010\tJ\b\u0010\u000e\u001a\u00020\u000fH\u0016J\u0018\u0010\u0010\u001a\u00020\b2\u0006\u0010\u0011\u001a\u00020\u00022\u0006\u0010\u0012\u001a\u00020\u000fH\u0016J\u0018\u0010\u0013\u001a\u00020\u00022\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u000fH\u0016R\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u001d\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0018"}, d2 = {"Lcom/example/stremiompvplayer/adapters/AddonListAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/example/stremiompvplayer/adapters/AddonListAdapter$ViewHolder;", "addons", "", "", "onDeleteClick", "Lkotlin/Function1;", "", "(Ljava/util/List;Lkotlin/jvm/functions/Function1;)V", "getAddons", "()Ljava/util/List;", "getOnDeleteClick", "()Lkotlin/jvm/functions/Function1;", "getItemCount", "", "onBindViewHolder", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "ViewHolder", "app_debug"})
public final class AddonListAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.example.stremiompvplayer.adapters.AddonListAdapter.ViewHolder> {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> addons = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<java.lang.String, kotlin.Unit> onDeleteClick = null;
    
    public AddonListAdapter(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> addons, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onDeleteClick) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getAddons() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlin.jvm.functions.Function1<java.lang.String, kotlin.Unit> getOnDeleteClick() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.example.stremiompvplayer.adapters.AddonListAdapter.ViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.example.stremiompvplayer.adapters.AddonListAdapter.ViewHolder holder, int position) {
    }
    
    @java.lang.Override()
    public int getItemCount() {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012J\u0010\u0010\u0013\u001a\u00020\u00122\u0006\u0010\u0014\u001a\u00020\u0012H\u0002R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\t\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\bR\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u0015"}, d2 = {"Lcom/example/stremiompvplayer/adapters/AddonListAdapter$ViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "(Landroid/view/View;)V", "addonNameText", "Landroid/widget/TextView;", "getAddonNameText", "()Landroid/widget/TextView;", "addonUrlText", "getAddonUrlText", "deleteButton", "Landroid/widget/ImageButton;", "getDeleteButton", "()Landroid/widget/ImageButton;", "bind", "", "addonUrl", "", "extractAddonName", "url", "app_debug"})
    public static final class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView addonUrlText = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView addonNameText = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.ImageButton deleteButton = null;
        
        public ViewHolder(@org.jetbrains.annotations.NotNull()
        android.view.View itemView) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getAddonUrlText() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getAddonNameText() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.ImageButton getDeleteButton() {
            return null;
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull()
        java.lang.String addonUrl) {
        }
        
        private final java.lang.String extractAddonName(java.lang.String url) {
            return null;
        }
    }
}