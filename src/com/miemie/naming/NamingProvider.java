package com.miemie.naming;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class NamingProvider extends ContentProvider {

  private static final String TAG = NamingProvider.class.getSimpleName();

  private SQLiteOpenHelper mOpenHelper;

  private static final int CHARS = 1;
  private static final int CHARS_ID = 2;
  private static final int PINYINS = 3;
  private static final int PINYINS_ID = 4;  
  
  private static final UriMatcher sURLMatcher = new UriMatcher(UriMatcher.NO_MATCH);

  private static final String TABLE_NAME1 = "characters";
  private static final String TABLE_NAME2 = "pinyin";

  /**
   * The content:// style URL for this table
   */
  public static final Uri CONTENT_URI1 = Uri.parse("content://com.miemie.naming/" + TABLE_NAME1);
  public static final Uri CONTENT_URI2 = Uri.parse("content://com.miemie.naming/" + TABLE_NAME2);

  static {
    sURLMatcher.addURI("com.miemie.naming", "characters", CHARS);
    sURLMatcher.addURI("com.miemie.naming", "characters/#", CHARS_ID);
    sURLMatcher.addURI("com.miemie.naming", "pinyin", PINYINS);
    sURLMatcher.addURI("com.miemie.naming", "pinyin/#", PINYINS_ID);    
  }

  private static class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "pinyin.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE characters (" + "_id INTEGER PRIMARY KEY," + "hanzi TEXT, "
          + "unicode INTEGER, " + "strokes INTEGER, " + "pinyin TEXT, " + "tone INTEGER);");
      db.execSQL("CREATE TABLE pinyin (" + "_id INTEGER PRIMARY KEY, " + "pinyin TEXT);");      
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME1);
      db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME2);
      onCreate(db);
    }
  }

  @Override
  public boolean onCreate() {
    mOpenHelper = new DatabaseHelper(getContext());
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
      String sortOrder) {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

    // Generate the body of the query
    int match = sURLMatcher.match(uri);
    switch (match) {
      case CHARS:
        qb.setTables(TABLE_NAME1);
        break;
      case CHARS_ID:
        qb.setTables(TABLE_NAME1);
        qb.appendWhere("_id=");
        qb.appendWhere(uri.getPathSegments().get(1));
        break;
      case PINYINS:
        qb.setTables(TABLE_NAME2);
        break;
      case PINYINS_ID:
        qb.setTables(TABLE_NAME2);
        qb.appendWhere("_id=");
        qb.appendWhere(uri.getPathSegments().get(1));
        break;        
      default:
        throw new IllegalArgumentException("Unknown URI " + uri);
    }

    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
    Cursor ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

    if (ret == null) {
      Log.e(TAG, "query: failed");
    } else {
      ret.setNotificationUri(getContext().getContentResolver(), uri);
    }

    return ret;
  }

  @Override
  public String getType(Uri uri) {
    int match = sURLMatcher.match(uri);
    switch (match) {
      case CHARS:
        return "vnd.android.cursor.dir/" + TABLE_NAME1;
      case CHARS_ID:
        return "vnd.android.cursor.item/" + TABLE_NAME1;
      case PINYINS:
        return "vnd.android.cursor.dir/" + TABLE_NAME2;
      case PINYINS_ID:
        return "vnd.android.cursor.item/" + TABLE_NAME2;        
      default:
        throw new IllegalArgumentException("Unknown URI");
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues initialValues) {
    String tableName = TABLE_NAME1;
    Uri contentUri = CONTENT_URI1;
    if (sURLMatcher.match(uri) == CHARS) {
      tableName = TABLE_NAME1;
      contentUri = CONTENT_URI1;
    } else if (sURLMatcher.match(uri) == PINYINS) {
      tableName = TABLE_NAME2;
      contentUri = CONTENT_URI2;
    } else {
      throw new IllegalArgumentException("Cannot insert into URI: " + uri);
    }

    ContentValues values;
    if (initialValues != null)
      values = new ContentValues(initialValues);
    else
      values = new ContentValues();

    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    long rowId = db.insert(tableName, null, values);
    if (rowId < 0) {
      throw new SQLException("Failed to insert row into " + uri);
    }

    Uri newUri = ContentUris.withAppendedId(contentUri, rowId);
    getContext().getContentResolver().notifyChange(newUri, null);
    return newUri;
  }

  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    int count;
    switch (sURLMatcher.match(uri)) {
      case CHARS:
        count = db.delete(TABLE_NAME1, where, whereArgs);
        break;
      case CHARS_ID: {
        String segment = uri.getPathSegments().get(1);
        if (TextUtils.isEmpty(where)) {
          where = "_id=" + segment;
        } else {
          where = "_id=" + segment + " AND (" + where + ")";
        }
        count = db.delete(TABLE_NAME1, where, whereArgs);
      }
        break;
      case PINYINS:
        count = db.delete(TABLE_NAME2, where, whereArgs);
        break;
      case PINYINS_ID: {
        String segment = uri.getPathSegments().get(1);
        if (TextUtils.isEmpty(where)) {
          where = "_id=" + segment;
        } else {
          where = "_id=" + segment + " AND (" + where + ")";
        }
        count = db.delete(TABLE_NAME2, where, whereArgs);
      }
        break;
      default:
        throw new IllegalArgumentException("Cannot delete from URI: " + uri);
    }

    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    int count;
    long rowId = 0;
    int match = sURLMatcher.match(uri);
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    switch (match) {
      case CHARS_ID: {
        String segment = uri.getPathSegments().get(1);
        rowId = Long.parseLong(segment);
        try {
          count = db.update(TABLE_NAME1, values, "_id=" + rowId, null);
        } catch (SQLException e) {
          Log.e(TAG, "update SQLException ", e);
          return 0;
        }
        break;
      }
      case PINYINS_ID: {
        String segment = uri.getPathSegments().get(1);
        rowId = Long.parseLong(segment);
        try {
          count = db.update(TABLE_NAME2, values, "_id=" + rowId, null);
        } catch (SQLException e) {
          Log.e(TAG, "update SQLException ", e);
          return 0;
        }
        break;
      }
      default: {
        throw new UnsupportedOperationException("Cannot update URI: " + uri);
      }
    }
    // Log.v(TAG, "*** notifyChange() rowId: " + rowId + " uri " + uri);
    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }

}
