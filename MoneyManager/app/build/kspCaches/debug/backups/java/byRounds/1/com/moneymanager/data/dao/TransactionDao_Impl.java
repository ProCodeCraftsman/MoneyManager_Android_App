package com.moneymanager.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.moneymanager.data.entity.TransactionEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TransactionDao_Impl implements TransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TransactionEntity> __insertionAdapterOfTransactionEntity;

  private final EntityDeletionOrUpdateAdapter<TransactionEntity> __deletionAdapterOfTransactionEntity;

  private final EntityDeletionOrUpdateAdapter<TransactionEntity> __updateAdapterOfTransactionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllTransactions;

  public TransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTransactionEntity = new EntityInsertionAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transactions` (`id`,`accountId`,`type`,`amount`,`categoryId`,`tagIds`,`date`,`note`,`receiptPath`,`isRecurring`,`recurringId`,`splitData`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getAccountId());
        statement.bindString(3, entity.getType());
        statement.bindDouble(4, entity.getAmount());
        if (entity.getCategoryId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getCategoryId());
        }
        statement.bindString(6, entity.getTagIds());
        statement.bindLong(7, entity.getDate());
        statement.bindString(8, entity.getNote());
        if (entity.getReceiptPath() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getReceiptPath());
        }
        final int _tmp = entity.isRecurring() ? 1 : 0;
        statement.bindLong(10, _tmp);
        if (entity.getRecurringId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getRecurringId());
        }
        if (entity.getSplitData() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getSplitData());
        }
        statement.bindLong(13, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfTransactionEntity = new EntityDeletionOrUpdateAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `transactions` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfTransactionEntity = new EntityDeletionOrUpdateAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `transactions` SET `id` = ?,`accountId` = ?,`type` = ?,`amount` = ?,`categoryId` = ?,`tagIds` = ?,`date` = ?,`note` = ?,`receiptPath` = ?,`isRecurring` = ?,`recurringId` = ?,`splitData` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getAccountId());
        statement.bindString(3, entity.getType());
        statement.bindDouble(4, entity.getAmount());
        if (entity.getCategoryId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getCategoryId());
        }
        statement.bindString(6, entity.getTagIds());
        statement.bindLong(7, entity.getDate());
        statement.bindString(8, entity.getNote());
        if (entity.getReceiptPath() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getReceiptPath());
        }
        final int _tmp = entity.isRecurring() ? 1 : 0;
        statement.bindLong(10, _tmp);
        if (entity.getRecurringId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getRecurringId());
        }
        if (entity.getSplitData() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getSplitData());
        }
        statement.bindLong(13, entity.getCreatedAt());
        statement.bindLong(14, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAllTransactions = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions";
        return _query;
      }
    };
  }

  @Override
  public Object insertTransaction(final TransactionEntity transaction,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTransactionEntity.insertAndReturnId(transaction);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTransaction(final TransactionEntity transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTransactionEntity.handle(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTransaction(final TransactionEntity transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTransactionEntity.handle(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllTransactions(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllTransactions.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllTransactions.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TransactionEntity>> getAllTransactions() {
    final String _sql = "SELECT * FROM transactions ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionEntity>> getTransactionsByDateRange(final long startDate,
      final long endDate) {
    final String _sql = "SELECT * FROM transactions WHERE date BETWEEN ? AND ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionEntity>> getTransactionsByAccount(final long accountId) {
    final String _sql = "SELECT * FROM transactions WHERE accountId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionEntity>> getTransactionsByType(final String type) {
    final String _sql = "SELECT * FROM transactions WHERE type = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, type);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionEntity>> getTransactionsByCategory(final long categoryId) {
    final String _sql = "SELECT * FROM transactions WHERE categoryId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, categoryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTransactionById(final long id,
      final Continuation<? super TransactionEntity> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TransactionEntity>() {
      @Override
      @Nullable
      public TransactionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final TransactionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
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

  @Override
  public Flow<Double> getTotalIncome(final long startDate, final long endDate) {
    final String _sql = "SELECT SUM(amount) FROM transactions WHERE type = 'income' AND date BETWEEN ? AND ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Double> getTotalExpense(final long startDate, final long endDate) {
    final String _sql = "SELECT SUM(amount) FROM transactions WHERE type = 'expense' AND date BETWEEN ? AND ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionEntity>> getRecentTransactions(final int limit) {
    final String _sql = "SELECT * FROM transactions ORDER BY date DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionEntity>> searchTransactions(final String query) {
    final String _sql = "SELECT * FROM transactions WHERE note LIKE '%' || ? || '%' ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionEntity>> getTransactionsByAccountAndType(final long accountId,
      final String type) {
    final String _sql = "SELECT * FROM transactions WHERE accountId = ? AND type = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindString(_argIndex, type);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionEntity>> getTransactionsByAccountAndCategory(final long accountId,
      final long categoryId) {
    final String _sql = "SELECT * FROM transactions WHERE accountId = ? AND categoryId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, categoryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionEntity>> getTransactionsByAccountAndDateRange(final long accountId,
      final long startDate, final long endDate) {
    final String _sql = "SELECT * FROM transactions WHERE accountId = ? AND date BETWEEN ? AND ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionEntity>> getTransactionsByTag(final long tagId) {
    final String _sql = "SELECT * FROM transactions WHERE tagIds LIKE '%' || ? || '%' ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, tagId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionEntity>> getTransactionsWithFilters(final Long accountId,
      final String type, final Long categoryId, final Long tagId, final Long startDate,
      final Long endDate) {
    final String _sql = "\n"
            + "        SELECT * FROM transactions \n"
            + "        WHERE (? IS NULL OR accountId = ?)\n"
            + "        AND (? IS NULL OR type = ?)\n"
            + "        AND (? IS NULL OR categoryId = ?)\n"
            + "        AND (? IS NULL OR tagIds LIKE '%' || ? || '%')\n"
            + "        AND (? IS NULL OR date >= ?)\n"
            + "        AND (? IS NULL OR date <= ?)\n"
            + "        ORDER BY date DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 12);
    int _argIndex = 1;
    if (accountId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, accountId);
    }
    _argIndex = 2;
    if (accountId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, accountId);
    }
    _argIndex = 3;
    if (type == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, type);
    }
    _argIndex = 4;
    if (type == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, type);
    }
    _argIndex = 5;
    if (categoryId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, categoryId);
    }
    _argIndex = 6;
    if (categoryId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, categoryId);
    }
    _argIndex = 7;
    if (tagId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, tagId);
    }
    _argIndex = 8;
    if (tagId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, tagId);
    }
    _argIndex = 9;
    if (startDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, startDate);
    }
    _argIndex = 10;
    if (startDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, startDate);
    }
    _argIndex = 11;
    if (endDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, endDate);
    }
    _argIndex = 12;
    if (endDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, endDate);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfTagIds = CursorUtil.getColumnIndexOrThrow(_cursor, "tagIds");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReceiptPath = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptPath");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringId");
          final int _cursorIndexOfSplitData = CursorUtil.getColumnIndexOrThrow(_cursor, "splitData");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpTagIds;
            _tmpTagIds = _cursor.getString(_cursorIndexOfTagIds);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReceiptPath;
            if (_cursor.isNull(_cursorIndexOfReceiptPath)) {
              _tmpReceiptPath = null;
            } else {
              _tmpReceiptPath = _cursor.getString(_cursorIndexOfReceiptPath);
            }
            final boolean _tmpIsRecurring;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp != 0;
            final Long _tmpRecurringId;
            if (_cursor.isNull(_cursorIndexOfRecurringId)) {
              _tmpRecurringId = null;
            } else {
              _tmpRecurringId = _cursor.getLong(_cursorIndexOfRecurringId);
            }
            final String _tmpSplitData;
            if (_cursor.isNull(_cursorIndexOfSplitData)) {
              _tmpSplitData = null;
            } else {
              _tmpSplitData = _cursor.getString(_cursorIndexOfSplitData);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpType,_tmpAmount,_tmpCategoryId,_tmpTagIds,_tmpDate,_tmpNote,_tmpReceiptPath,_tmpIsRecurring,_tmpRecurringId,_tmpSplitData,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
