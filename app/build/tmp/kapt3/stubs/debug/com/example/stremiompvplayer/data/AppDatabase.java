package com.example.stremiompvplayer.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\'\u0018\u0000 \u00122\u00020\u0001:\u0002\u0011\u0012B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\bH&J\b\u0010\t\u001a\u00020\nH&J\b\u0010\u000b\u001a\u00020\fH&J\b\u0010\r\u001a\u00020\u000eH&J\b\u0010\u000f\u001a\u00020\u0010H&\u00a8\u0006\u0013"}, d2 = {"Lcom/example/stremiompvplayer/data/AppDatabase;", "Landroidx/room/RoomDatabase;", "()V", "feedListDao", "Lcom/example/stremiompvplayer/data/FeedListDao;", "hubSlotDao", "Lcom/example/stremiompvplayer/data/HubSlotDao;", "libraryItemDao", "Lcom/example/stremiompvplayer/data/LibraryItemDao;", "nextUpItemDao", "Lcom/example/stremiompvplayer/data/NextUpItemDao;", "userDao", "Lcom/example/stremiompvplayer/data/UserDao;", "userSettingsDao", "Lcom/example/stremiompvplayer/data/UserSettingsDao;", "watchProgressDao", "Lcom/example/stremiompvplayer/data/WatchProgressDao;", "AppDatabaseCallback", "Companion", "app_debug"})
@androidx.room.Database(entities = {com.example.stremiompvplayer.models.FeedList.class, com.example.stremiompvplayer.models.HubSlot.class, com.example.stremiompvplayer.models.User.class, com.example.stremiompvplayer.models.LibraryItem.class, com.example.stremiompvplayer.models.WatchProgress.class, com.example.stremiompvplayer.models.NextUpItem.class, com.example.stremiompvplayer.models.UserSettings.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends androidx.room.RoomDatabase {
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile com.example.stremiompvplayer.data.AppDatabase INSTANCE;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.stremiompvplayer.data.AppDatabase.Companion Companion = null;
    
    public AppDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.stremiompvplayer.data.UserDao userDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.stremiompvplayer.data.LibraryItemDao libraryItemDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.stremiompvplayer.data.WatchProgressDao watchProgressDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.stremiompvplayer.data.NextUpItemDao nextUpItemDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.stremiompvplayer.data.UserSettingsDao userSettingsDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.stremiompvplayer.data.FeedListDao feedListDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.example.stremiompvplayer.data.HubSlotDao hubSlotDao();
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0016J\u0016\u0010\t\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u000bH\u0086@\u00a2\u0006\u0002\u0010\fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/example/stremiompvplayer/data/AppDatabase$AppDatabaseCallback;", "Landroidx/room/RoomDatabase$Callback;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "(Lkotlinx/coroutines/CoroutineScope;)V", "onCreate", "", "db", "Landroidx/sqlite/db/SupportSQLiteDatabase;", "populateDatabase", "database", "Lcom/example/stremiompvplayer/data/AppDatabase;", "(Lcom/example/stremiompvplayer/data/AppDatabase;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
    static final class AppDatabaseCallback extends androidx.room.RoomDatabase.Callback {
        @org.jetbrains.annotations.NotNull()
        private final kotlinx.coroutines.CoroutineScope scope = null;
        
        public AppDatabaseCallback(@org.jetbrains.annotations.NotNull()
        kotlinx.coroutines.CoroutineScope scope) {
            super();
        }
        
        @java.lang.Override()
        public void onCreate(@org.jetbrains.annotations.NotNull()
        androidx.sqlite.db.SupportSQLiteDatabase db) {
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.Object populateDatabase(@org.jetbrains.annotations.NotNull()
        com.example.stremiompvplayer.data.AppDatabase database, @org.jetbrains.annotations.NotNull()
        kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tR\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/example/stremiompvplayer/data/AppDatabase$Companion;", "", "()V", "INSTANCE", "Lcom/example/stremiompvplayer/data/AppDatabase;", "getDatabase", "context", "Landroid/content/Context;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.stremiompvplayer.data.AppDatabase getDatabase(@org.jetbrains.annotations.NotNull()
        android.content.Context context, @org.jetbrains.annotations.NotNull()
        kotlinx.coroutines.CoroutineScope scope) {
            return null;
        }
    }
}