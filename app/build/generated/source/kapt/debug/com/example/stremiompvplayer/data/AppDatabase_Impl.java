package com.example.stremiompvplayer.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile UserDao _userDao;

  private volatile LibraryItemDao _libraryItemDao;

  private volatile WatchProgressDao _watchProgressDao;

  private volatile NextUpItemDao _nextUpItemDao;

  private volatile UserSettingsDao _userSettingsDao;

  private volatile FeedListDao _feedListDao;

  private volatile HubSlotDao _hubSlotDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `FeedList` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `catalogUrl` TEXT NOT NULL, `type` TEXT NOT NULL, `catalogId` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `addedToMovieHub` INTEGER NOT NULL, `addedToSeriesHub` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `HubSlot` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `hubType` TEXT NOT NULL, `slotIndex` INTEGER NOT NULL, `feedListId` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `User` (`authKey` TEXT NOT NULL, `email` TEXT NOT NULL, `avatar` TEXT NOT NULL, `isGuest` INTEGER NOT NULL, PRIMARY KEY(`authKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `LibraryItem` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `name` TEXT NOT NULL, `poster` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `WatchProgress` (`id` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `NextUpItem` (`id` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `UserSettings` (`id` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '424e3aa20cc4e86bdf7e3eb4c56b3f3a')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `FeedList`");
        db.execSQL("DROP TABLE IF EXISTS `HubSlot`");
        db.execSQL("DROP TABLE IF EXISTS `User`");
        db.execSQL("DROP TABLE IF EXISTS `LibraryItem`");
        db.execSQL("DROP TABLE IF EXISTS `WatchProgress`");
        db.execSQL("DROP TABLE IF EXISTS `NextUpItem`");
        db.execSQL("DROP TABLE IF EXISTS `UserSettings`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsFeedList = new HashMap<String, TableInfo.Column>(9);
        _columnsFeedList.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFeedList.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFeedList.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFeedList.put("catalogUrl", new TableInfo.Column("catalogUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFeedList.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFeedList.put("catalogId", new TableInfo.Column("catalogId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFeedList.put("orderIndex", new TableInfo.Column("orderIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFeedList.put("addedToMovieHub", new TableInfo.Column("addedToMovieHub", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFeedList.put("addedToSeriesHub", new TableInfo.Column("addedToSeriesHub", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFeedList = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFeedList = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFeedList = new TableInfo("FeedList", _columnsFeedList, _foreignKeysFeedList, _indicesFeedList);
        final TableInfo _existingFeedList = TableInfo.read(db, "FeedList");
        if (!_infoFeedList.equals(_existingFeedList)) {
          return new RoomOpenHelper.ValidationResult(false, "FeedList(com.example.stremiompvplayer.models.FeedList).\n"
                  + " Expected:\n" + _infoFeedList + "\n"
                  + " Found:\n" + _existingFeedList);
        }
        final HashMap<String, TableInfo.Column> _columnsHubSlot = new HashMap<String, TableInfo.Column>(5);
        _columnsHubSlot.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHubSlot.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHubSlot.put("hubType", new TableInfo.Column("hubType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHubSlot.put("slotIndex", new TableInfo.Column("slotIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHubSlot.put("feedListId", new TableInfo.Column("feedListId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHubSlot = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHubSlot = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHubSlot = new TableInfo("HubSlot", _columnsHubSlot, _foreignKeysHubSlot, _indicesHubSlot);
        final TableInfo _existingHubSlot = TableInfo.read(db, "HubSlot");
        if (!_infoHubSlot.equals(_existingHubSlot)) {
          return new RoomOpenHelper.ValidationResult(false, "HubSlot(com.example.stremiompvplayer.models.HubSlot).\n"
                  + " Expected:\n" + _infoHubSlot + "\n"
                  + " Found:\n" + _existingHubSlot);
        }
        final HashMap<String, TableInfo.Column> _columnsUser = new HashMap<String, TableInfo.Column>(4);
        _columnsUser.put("authKey", new TableInfo.Column("authKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("email", new TableInfo.Column("email", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("avatar", new TableInfo.Column("avatar", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("isGuest", new TableInfo.Column("isGuest", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUser = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUser = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUser = new TableInfo("User", _columnsUser, _foreignKeysUser, _indicesUser);
        final TableInfo _existingUser = TableInfo.read(db, "User");
        if (!_infoUser.equals(_existingUser)) {
          return new RoomOpenHelper.ValidationResult(false, "User(com.example.stremiompvplayer.models.User).\n"
                  + " Expected:\n" + _infoUser + "\n"
                  + " Found:\n" + _existingUser);
        }
        final HashMap<String, TableInfo.Column> _columnsLibraryItem = new HashMap<String, TableInfo.Column>(4);
        _columnsLibraryItem.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryItem.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryItem.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryItem.put("poster", new TableInfo.Column("poster", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLibraryItem = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLibraryItem = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLibraryItem = new TableInfo("LibraryItem", _columnsLibraryItem, _foreignKeysLibraryItem, _indicesLibraryItem);
        final TableInfo _existingLibraryItem = TableInfo.read(db, "LibraryItem");
        if (!_infoLibraryItem.equals(_existingLibraryItem)) {
          return new RoomOpenHelper.ValidationResult(false, "LibraryItem(com.example.stremiompvplayer.models.LibraryItem).\n"
                  + " Expected:\n" + _infoLibraryItem + "\n"
                  + " Found:\n" + _existingLibraryItem);
        }
        final HashMap<String, TableInfo.Column> _columnsWatchProgress = new HashMap<String, TableInfo.Column>(1);
        _columnsWatchProgress.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWatchProgress = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWatchProgress = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWatchProgress = new TableInfo("WatchProgress", _columnsWatchProgress, _foreignKeysWatchProgress, _indicesWatchProgress);
        final TableInfo _existingWatchProgress = TableInfo.read(db, "WatchProgress");
        if (!_infoWatchProgress.equals(_existingWatchProgress)) {
          return new RoomOpenHelper.ValidationResult(false, "WatchProgress(com.example.stremiompvplayer.models.WatchProgress).\n"
                  + " Expected:\n" + _infoWatchProgress + "\n"
                  + " Found:\n" + _existingWatchProgress);
        }
        final HashMap<String, TableInfo.Column> _columnsNextUpItem = new HashMap<String, TableInfo.Column>(1);
        _columnsNextUpItem.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNextUpItem = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesNextUpItem = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoNextUpItem = new TableInfo("NextUpItem", _columnsNextUpItem, _foreignKeysNextUpItem, _indicesNextUpItem);
        final TableInfo _existingNextUpItem = TableInfo.read(db, "NextUpItem");
        if (!_infoNextUpItem.equals(_existingNextUpItem)) {
          return new RoomOpenHelper.ValidationResult(false, "NextUpItem(com.example.stremiompvplayer.models.NextUpItem).\n"
                  + " Expected:\n" + _infoNextUpItem + "\n"
                  + " Found:\n" + _existingNextUpItem);
        }
        final HashMap<String, TableInfo.Column> _columnsUserSettings = new HashMap<String, TableInfo.Column>(1);
        _columnsUserSettings.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserSettings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserSettings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserSettings = new TableInfo("UserSettings", _columnsUserSettings, _foreignKeysUserSettings, _indicesUserSettings);
        final TableInfo _existingUserSettings = TableInfo.read(db, "UserSettings");
        if (!_infoUserSettings.equals(_existingUserSettings)) {
          return new RoomOpenHelper.ValidationResult(false, "UserSettings(com.example.stremiompvplayer.models.UserSettings).\n"
                  + " Expected:\n" + _infoUserSettings + "\n"
                  + " Found:\n" + _existingUserSettings);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "424e3aa20cc4e86bdf7e3eb4c56b3f3a", "03bca3ebb8732b591d92cc8841cbafc4");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "FeedList","HubSlot","User","LibraryItem","WatchProgress","NextUpItem","UserSettings");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `FeedList`");
      _db.execSQL("DELETE FROM `HubSlot`");
      _db.execSQL("DELETE FROM `User`");
      _db.execSQL("DELETE FROM `LibraryItem`");
      _db.execSQL("DELETE FROM `WatchProgress`");
      _db.execSQL("DELETE FROM `NextUpItem`");
      _db.execSQL("DELETE FROM `UserSettings`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LibraryItemDao.class, LibraryItemDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WatchProgressDao.class, WatchProgressDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(NextUpItemDao.class, NextUpItemDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UserSettingsDao.class, UserSettingsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FeedListDao.class, FeedListDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HubSlotDao.class, HubSlotDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public UserDao userDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public LibraryItemDao libraryItemDao() {
    if (_libraryItemDao != null) {
      return _libraryItemDao;
    } else {
      synchronized(this) {
        if(_libraryItemDao == null) {
          _libraryItemDao = new LibraryItemDao_Impl(this);
        }
        return _libraryItemDao;
      }
    }
  }

  @Override
  public WatchProgressDao watchProgressDao() {
    if (_watchProgressDao != null) {
      return _watchProgressDao;
    } else {
      synchronized(this) {
        if(_watchProgressDao == null) {
          _watchProgressDao = new WatchProgressDao_Impl(this);
        }
        return _watchProgressDao;
      }
    }
  }

  @Override
  public NextUpItemDao nextUpItemDao() {
    if (_nextUpItemDao != null) {
      return _nextUpItemDao;
    } else {
      synchronized(this) {
        if(_nextUpItemDao == null) {
          _nextUpItemDao = new NextUpItemDao_Impl(this);
        }
        return _nextUpItemDao;
      }
    }
  }

  @Override
  public UserSettingsDao userSettingsDao() {
    if (_userSettingsDao != null) {
      return _userSettingsDao;
    } else {
      synchronized(this) {
        if(_userSettingsDao == null) {
          _userSettingsDao = new UserSettingsDao_Impl(this);
        }
        return _userSettingsDao;
      }
    }
  }

  @Override
  public FeedListDao feedListDao() {
    if (_feedListDao != null) {
      return _feedListDao;
    } else {
      synchronized(this) {
        if(_feedListDao == null) {
          _feedListDao = new FeedListDao_Impl(this);
        }
        return _feedListDao;
      }
    }
  }

  @Override
  public HubSlotDao hubSlotDao() {
    if (_hubSlotDao != null) {
      return _hubSlotDao;
    } else {
      synchronized(this) {
        if(_hubSlotDao == null) {
          _hubSlotDao = new HubSlotDao_Impl(this);
        }
        return _hubSlotDao;
      }
    }
  }
}
