package com.example.stremiompvplayer;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stremiompvplayer.databinding.ActivityUserSelectionBinding;
import com.example.stremiompvplayer.utils.SharedPreferencesManager;
import com.example.stremiompvplayer.utils.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0002\u0018\u00002\u00020\u0001:\u0001\u001bB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0002J\b\u0010\r\u001a\u00020\u000eH\u0002J\b\u0010\u000f\u001a\u00020\nH\u0002J\u0012\u0010\u0010\u001a\u00020\n2\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0014J\u0010\u0010\u0013\u001a\u00020\n2\u0006\u0010\u0014\u001a\u00020\u0015H\u0002J\b\u0010\u0016\u001a\u00020\nH\u0002J\b\u0010\u0017\u001a\u00020\nH\u0002J\u0012\u0010\u0018\u001a\u00020\n2\b\b\u0002\u0010\u0019\u001a\u00020\u001aH\u0002R\u0012\u0010\u0003\u001a\u00060\u0004R\u00020\u0000X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/example/stremiompvplayer/UserSelectionActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "adapter", "Lcom/example/stremiompvplayer/UserSelectionActivity$UserAdapter;", "binding", "Lcom/example/stremiompvplayer/databinding/ActivityUserSelectionBinding;", "prefsManager", "Lcom/example/stremiompvplayer/utils/SharedPreferencesManager;", "createUser", "", "name", "", "generateAvatarColor", "", "loadUsers", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "selectUser", "user", "Lcom/example/stremiompvplayer/utils/User;", "setupListeners", "setupRecyclerView", "showAddUserDialog", "isFirstUser", "", "UserAdapter", "app_debug"})
public final class UserSelectionActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.example.stremiompvplayer.databinding.ActivityUserSelectionBinding binding;
    private com.example.stremiompvplayer.utils.SharedPreferencesManager prefsManager;
    private com.example.stremiompvplayer.UserSelectionActivity.UserAdapter adapter;
    
    public UserSelectionActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupRecyclerView() {
    }
    
    private final void setupListeners() {
    }
    
    private final void loadUsers() {
    }
    
    private final void showAddUserDialog(boolean isFirstUser) {
    }
    
    private final void createUser(java.lang.String name) {
    }
    
    private final void selectUser(com.example.stremiompvplayer.utils.User user) {
    }
    
    private final int generateAvatarColor() {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010!\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0003\b\u0086\u0004\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0002\u001b\u001cB\u0019\u0012\u0012\u0010\u0003\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00060\u0004\u00a2\u0006\u0002\u0010\u0007J\b\u0010\u000f\u001a\u00020\tH\u0016J\u0010\u0010\u0010\u001a\u00020\t2\u0006\u0010\u0011\u001a\u00020\tH\u0016J\u0018\u0010\u0012\u001a\u00020\u00062\u0006\u0010\u0013\u001a\u00020\u00022\u0006\u0010\u0011\u001a\u00020\tH\u0016J\u0018\u0010\u0014\u001a\u00020\u00022\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\tH\u0016J\u001c\u0010\u0018\u001a\u00020\u00062\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00050\u001a2\u0006\u0010\u000b\u001a\u00020\fR\u000e\u0010\b\u001a\u00020\tX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\tX\u0082D\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0003\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00060\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00050\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001d"}, d2 = {"Lcom/example/stremiompvplayer/UserSelectionActivity$UserAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "onUserClick", "Lkotlin/Function1;", "Lcom/example/stremiompvplayer/utils/User;", "", "(Lcom/example/stremiompvplayer/UserSelectionActivity;Lkotlin/jvm/functions/Function1;)V", "TYPE_ADD", "", "TYPE_USER", "showAddButton", "", "users", "", "getItemCount", "getItemViewType", "position", "onBindViewHolder", "holder", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "setUsers", "newUsers", "", "AddUserViewHolder", "UserViewHolder", "app_debug"})
    public final class UserAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder> {
        @org.jetbrains.annotations.NotNull()
        private final kotlin.jvm.functions.Function1<com.example.stremiompvplayer.utils.User, kotlin.Unit> onUserClick = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<com.example.stremiompvplayer.utils.User> users = null;
        private boolean showAddButton = false;
        private final int TYPE_USER = 0;
        private final int TYPE_ADD = 1;
        
        public UserAdapter(@org.jetbrains.annotations.NotNull()
        kotlin.jvm.functions.Function1<? super com.example.stremiompvplayer.utils.User, kotlin.Unit> onUserClick) {
            super();
        }
        
        public final void setUsers(@org.jetbrains.annotations.NotNull()
        java.util.List<com.example.stremiompvplayer.utils.User> newUsers, boolean showAddButton) {
        }
        
        @java.lang.Override()
        public int getItemCount() {
            return 0;
        }
        
        @java.lang.Override()
        public int getItemViewType(int position) {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
        android.view.ViewGroup parent, int viewType) {
            return null;
        }
        
        @java.lang.Override()
        public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
        androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\t\u001a\u00020\nR\u000e\u0010\u0005\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/example/stremiompvplayer/UserSelectionActivity$UserAdapter$AddUserViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "(Lcom/example/stremiompvplayer/UserSelectionActivity$UserAdapter;Landroid/view/View;)V", "avatarContainer", "avatarInitial", "Landroid/widget/TextView;", "userName", "bind", "", "app_debug"})
        public final class AddUserViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            @org.jetbrains.annotations.NotNull()
            private final android.view.View avatarContainer = null;
            @org.jetbrains.annotations.NotNull()
            private final android.widget.TextView avatarInitial = null;
            @org.jetbrains.annotations.NotNull()
            private final android.widget.TextView userName = null;
            
            public AddUserViewHolder(@org.jetbrains.annotations.NotNull()
            android.view.View itemView) {
                super(null);
            }
            
            public final void bind() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fR\u000e\u0010\u0005\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/example/stremiompvplayer/UserSelectionActivity$UserAdapter$UserViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "(Lcom/example/stremiompvplayer/UserSelectionActivity$UserAdapter;Landroid/view/View;)V", "avatarContainer", "avatarInitial", "Landroid/widget/TextView;", "userName", "bind", "", "user", "Lcom/example/stremiompvplayer/utils/User;", "app_debug"})
        public final class UserViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            @org.jetbrains.annotations.NotNull()
            private final android.view.View avatarContainer = null;
            @org.jetbrains.annotations.NotNull()
            private final android.widget.TextView avatarInitial = null;
            @org.jetbrains.annotations.NotNull()
            private final android.widget.TextView userName = null;
            
            public UserViewHolder(@org.jetbrains.annotations.NotNull()
            android.view.View itemView) {
                super(null);
            }
            
            public final void bind(@org.jetbrains.annotations.NotNull()
            com.example.stremiompvplayer.utils.User user) {
            }
        }
    }
}