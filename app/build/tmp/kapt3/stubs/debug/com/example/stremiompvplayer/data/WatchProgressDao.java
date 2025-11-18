package com.example.stremiompvplayer.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.stremiompvplayer.models.LibraryItem;
import com.example.stremiompvplayer.models.NextUpItem;
import com.example.stremiompvplayer.models.User;
import com.example.stremiompvplayer.models.UserSettings;
import com.example.stremiompvplayer.models.WatchProgress;
import kotlinx.coroutines.Dispatchers;
import com.example.stremiompvplayer.models.FeedList;
import com.example.stremiompvplayer.models.HubSlot;
import com.example.stremiompvplayer.models.UserCatalog;
import com.example.stremiompvplayer.models.CollectedItem;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\bg\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\'\u00a8\u0006\u0005"}, d2 = {"Lcom/example/stremiompvplayer/data/WatchProgressDao;", "", "getAll", "", "Lcom/example/stremiompvplayer/models/WatchProgress;", "app_debug"})
@androidx.room.Dao()
public abstract interface WatchProgressDao {
    
    @androidx.room.Query(value = "SELECT * FROM WatchProgress")
    @org.jetbrains.annotations.NotNull()
    public abstract java.util.List<com.example.stremiompvplayer.models.WatchProgress> getAll();
}