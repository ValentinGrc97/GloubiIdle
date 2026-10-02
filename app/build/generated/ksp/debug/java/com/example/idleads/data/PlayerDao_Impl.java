package com.example.idleads.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PlayerDao_Impl implements PlayerDao {
  private final RoomDatabase __db;

  private final EntityUpsertionAdapter<Player> __upsertionAdapterOfPlayer;

  public PlayerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__upsertionAdapterOfPlayer = new EntityUpsertionAdapter<Player>(new EntityInsertionAdapter<Player>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `player` (`id`,`satiety`,`progress`,`lastSeen`,`boostUntil`,`adsToday`,`adsDay`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Player entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getSatiety());
        statement.bindDouble(3, entity.getProgress());
        statement.bindLong(4, entity.getLastSeen());
        statement.bindLong(5, entity.getBoostUntil());
        statement.bindLong(6, entity.getAdsToday());
        statement.bindLong(7, entity.getAdsDay());
      }
    }, new EntityDeletionOrUpdateAdapter<Player>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `player` SET `id` = ?,`satiety` = ?,`progress` = ?,`lastSeen` = ?,`boostUntil` = ?,`adsToday` = ?,`adsDay` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Player entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getSatiety());
        statement.bindDouble(3, entity.getProgress());
        statement.bindLong(4, entity.getLastSeen());
        statement.bindLong(5, entity.getBoostUntil());
        statement.bindLong(6, entity.getAdsToday());
        statement.bindLong(7, entity.getAdsDay());
        statement.bindLong(8, entity.getId());
      }
    });
  }

  @Override
  public Object save(final Player player, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfPlayer.upsert(player);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object get(final Continuation<? super Player> $completion) {
    final String _sql = "SELECT * FROM player WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Player>() {
      @Override
      @Nullable
      public Player call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSatiety = CursorUtil.getColumnIndexOrThrow(_cursor, "satiety");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfBoostUntil = CursorUtil.getColumnIndexOrThrow(_cursor, "boostUntil");
          final int _cursorIndexOfAdsToday = CursorUtil.getColumnIndexOrThrow(_cursor, "adsToday");
          final int _cursorIndexOfAdsDay = CursorUtil.getColumnIndexOrThrow(_cursor, "adsDay");
          final Player _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final double _tmpSatiety;
            _tmpSatiety = _cursor.getDouble(_cursorIndexOfSatiety);
            final double _tmpProgress;
            _tmpProgress = _cursor.getDouble(_cursorIndexOfProgress);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final long _tmpBoostUntil;
            _tmpBoostUntil = _cursor.getLong(_cursorIndexOfBoostUntil);
            final int _tmpAdsToday;
            _tmpAdsToday = _cursor.getInt(_cursorIndexOfAdsToday);
            final long _tmpAdsDay;
            _tmpAdsDay = _cursor.getLong(_cursorIndexOfAdsDay);
            _result = new Player(_tmpId,_tmpSatiety,_tmpProgress,_tmpLastSeen,_tmpBoostUntil,_tmpAdsToday,_tmpAdsDay);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
