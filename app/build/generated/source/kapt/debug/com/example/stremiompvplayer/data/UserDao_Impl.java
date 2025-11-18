package com.example.stremiompvplayer.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.stremiompvplayer.models.User;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class UserDao_Impl implements UserDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<User> __insertionAdapterOfUser;

  public UserDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUser = new EntityInsertionAdapter<User>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `User` (`authKey`,`email`,`avatar`,`isGuest`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final User entity) {
        if (entity.getAuthKey() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getAuthKey());
        }
        if (entity.getEmail() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getEmail());
        }
        if (entity.getAvatar() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getAvatar());
        }
        final int _tmp = entity.isGuest() ? 1 : 0;
        statement.bindLong(4, _tmp);
      }
    };
  }

  @Override
  public void insert(final User user) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfUser.insert(user);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<User> getAll() {
    final String _sql = "SELECT * FROM User";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfAuthKey = CursorUtil.getColumnIndexOrThrow(_cursor, "authKey");
      final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
      final int _cursorIndexOfAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "avatar");
      final int _cursorIndexOfIsGuest = CursorUtil.getColumnIndexOrThrow(_cursor, "isGuest");
      final List<User> _result = new ArrayList<User>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final User _item;
        final String _tmpAuthKey;
        if (_cursor.isNull(_cursorIndexOfAuthKey)) {
          _tmpAuthKey = null;
        } else {
          _tmpAuthKey = _cursor.getString(_cursorIndexOfAuthKey);
        }
        final String _tmpEmail;
        if (_cursor.isNull(_cursorIndexOfEmail)) {
          _tmpEmail = null;
        } else {
          _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
        }
        final String _tmpAvatar;
        if (_cursor.isNull(_cursorIndexOfAvatar)) {
          _tmpAvatar = null;
        } else {
          _tmpAvatar = _cursor.getString(_cursorIndexOfAvatar);
        }
        final boolean _tmpIsGuest;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsGuest);
        _tmpIsGuest = _tmp != 0;
        _item = new User(_tmpAuthKey,_tmpEmail,_tmpAvatar,_tmpIsGuest);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
