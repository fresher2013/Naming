package com.miemie.naming;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;


public class Utils {
  
  private final static String TAG = Utils.class.getSimpleName();
  
  private static int parse(char c) {
    if (c >= 'a') return (c - 'a' + 10) & 0x0f;
    if (c >= 'A') return (c - 'A' + 10) & 0x0f;
    return (c - '0') & 0x0f;
  }

  public static byte[] HexString2Bytes(String hexstr) {
    byte[] b = new byte[hexstr.length() / 2];
    int j = 0;
    for (int i = 0; i < b.length; i++) {
      char c0 = hexstr.charAt(j++);
      char c1 = hexstr.charAt(j++);
      b[i] = (byte) ((parse(c0) << 4) | parse(c1));
    }
    return b;
  }

  public static char HexString2Char(String hexstr) {
    byte[] buf = HexString2Bytes(hexstr);
    char c = 0;
    if (buf.length == 2) {
      c = (char) ((buf[1] & 0xff) | ((buf[0] & 0xff) << 8));
    }
    return c;
  }

  private static final String DATABASE_FILENAME = "pinyin.db";

  public static SQLiteDatabase openDatabase(Context context) {
    try {
      File file = new File(context.getFilesDir(), DATABASE_FILENAME);

      if (!file.exists()) {
        file.createNewFile();
        InputStream is = context.getResources().openRawResource(R.raw.pinyin);
        FileOutputStream fos = new FileOutputStream(file);

        byte[] buffer = new byte[8192];
        int count = 0;

        while ((count = is.read(buffer)) > 0) {
          fos.write(buffer, 0, count);
        }
        fos.close();
        is.close();
      }

      SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(file, null);
      return database;
    } catch (Exception e) {} finally {}

    return null;
  }

  private static final String DICT_DATABASE_FILENAME = "zidian.db";
  public static SQLiteDatabase openDictDatabase(Context context) {
    try {
      File file = new File(context.getFilesDir(), DICT_DATABASE_FILENAME);

      if (!file.exists()) {
        file.createNewFile();
        InputStream is = context.getResources().openRawResource(R.raw.zidian);
        FileOutputStream fos = new FileOutputStream(file);

        byte[] buffer = new byte[8192];
        int count = 0;

        while ((count = is.read(buffer)) > 0) {
          fos.write(buffer, 0, count);
        }
        fos.close();
        is.close();
      }

      SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(file, null);
      return database;
    } catch (Exception e) {} finally {}

    return null;
  }
  
  public static List<String> query(Context context, int id) {

    SharedPreferences pref = context.getSharedPreferences(Constant.PREF_NAME, Context.MODE_PRIVATE);
    SQLiteDatabase db = openDatabase(context);

    String pinyin = Utils.getStringPrefValue(pref, Constant.PREF_PINYIN, id);
    int maxStrokes = Utils.getIntPrefValue(pref, Constant.PREF_MAX_STROKE, id);
    int minStrokes = Utils.getIntPrefValue(pref, Constant.PREF_MIN_STROKE, id);
    int tone = Utils.getIntPrefValue(pref, Constant.PREF_TONE, id);

    HashSet<String> pinyins = new HashSet<String>();
    if (TextUtils.isEmpty(pinyin)) {
      Cursor c = db.rawQuery("select * from pinyin", null);

      if (c != null) {
        
        if (c.getCount() > 0) {
          c.moveToFirst();
          do {
            pinyins.add(c.getString(1));
          } while (c.moveToNext());
        }
        c.close();
      }

    } else {
//      Log.e(TAG, "" + pinyin);      
      String[] apinyins = pinyin.split("@");
      for(String str:apinyins){
        pinyins.add(str);
      }
    }

    StringBuilder query = new StringBuilder("select hanzi, pinyin from characters");
    query.append(" where ");

    if (maxStrokes > 0 || minStrokes > 0) {
      query.append("( ");
      if (minStrokes > 0) {
        query.append("strokes >= ");
        query.append(minStrokes);
        if (maxStrokes > 0) query.append(" and ");
      }
      if (maxStrokes > 0) {
        query.append(" strokes <= ");
        query.append(maxStrokes);
      }
      query.append(" ) and ");
    }

    if (tone > 0) {
      query.append(" ( ");
      boolean first = true;
      if ((tone & Constant.TONE_1) == Constant.TONE_1) {
        query.append("tone=1");
        first = false;
      }
      if ((tone & Constant.TONE_2) == Constant.TONE_2) {
        if (!first) query.append(" or ");
        query.append("tone=2");
        first = false;
      }
      if ((tone & Constant.TONE_3) == Constant.TONE_3) {
        if (!first) query.append(" or ");
        query.append("tone=3");
        first = false;
      }
      if ((tone & Constant.TONE_4) == Constant.TONE_4) {
        if (!first) query.append(" or ");
        query.append("tone=4");
        first = false;
      }
      query.append(" ) and ");
    }

    query.append("( popular = 1 )");
    
    Log.d(TAG, query.toString());

    Cursor c = db.rawQuery(query.toString(), null);

    if (c != null) {
      HashSet<String> chars = new HashSet<String>();
      Log.e(TAG, ""+c.getCount());
      if (c.getCount() > 0) {        
        c.moveToFirst();
        do {
          if (pinyins.contains(c.getString(1)) && !chars.contains(c.getString(0))) {
            Log.e(TAG, "" + c.getString(1) + " " + c.getString(0));
            chars.add(c.getString(0));
          }
        } while (c.moveToNext());
      }
      c.close();
      
      ArrayList<String> ret = new ArrayList<String>();
      ret.addAll(chars);
      return ret;
    }

    return null;
  }  

  public static List<String> doParse(InputStream in) {
    List<String> persons = null;
    String person = null;

    String tagName;

    XmlPullParser parser = Xml.newPullParser();
    try {
      parser.setInput(in, "utf-8");
      int eventType = parser.getEventType();

      while (eventType != XmlPullParser.END_DOCUMENT) {
        switch (eventType) {
          case XmlPullParser.START_DOCUMENT:
            persons = new ArrayList<String>();
            break;
          case XmlPullParser.START_TAG:
            tagName = parser.getName();
            if ("Hanyu".equals(tagName)) {
              person = parser.nextText();
              Log.e("utils", person);
            }
            break;
          case XmlPullParser.END_TAG:
            if ("Hanyu".equals(parser.getName())) {
              persons.add(person);
            }
            break;
        }
        eventType = parser.next();
      }

    } catch (XmlPullParserException e) {} catch (IOException e) {}
    return persons;
  }

  public static String getStringPrefValue(SharedPreferences pref, String key, int id) {
    StringBuilder sb = new StringBuilder(key);
    if (id != 0) sb.append(id);

    return pref.getString(sb.toString(), null);
  }

  public static int getIntPrefValue(SharedPreferences pref, String key, int id) {
    StringBuilder sb = new StringBuilder(key);
    if (id != 0) sb.append(id);
    return pref.getInt(sb.toString(), 0);
  }
  
  public static boolean saveStringPrefValue(SharedPreferences pref, String key, String value, int id) {

    if (pref != null) {
      StringBuilder sb = new StringBuilder(key);
      if (id != 0) sb.append(id);
      SharedPreferences.Editor editor = pref.edit();
      editor.putString(sb.toString(), value);
      editor.apply();
      return true;
    }

    return false;
  }

  public static boolean saveIntPrefValue(SharedPreferences pref, String key, int value, int id) {
    if (pref != null) {
      StringBuilder sb = new StringBuilder(key);
      if (id != 0) sb.append(id);
      SharedPreferences.Editor editor = pref.edit();
      editor.putInt(sb.toString(), value);
      editor.apply();
      return true;
    }

    return false;
  }
  
  public static File getAppDir() {
    File dir = new File(android.os.Environment.getExternalStorageDirectory(), "Naming");
    if (!dir.exists()) {
      dir.mkdir();
    }
    return dir;
  }
  
  public static void updateDB(Context context) {
    SQLiteDatabase db = openDatabase(context);
    Scanner scanner = null;
    try {
      scanner = new Scanner(context.getAssets().open("name_mostused.txt"));

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (!TextUtils.isEmpty(line)) {

          Cursor cursor =
              db.rawQuery("select _id,hanzi from characters where hanzi=?", new String[] {line});
          if (cursor != null) {
            if (cursor.getCount() > 0) {
              cursor.moveToFirst();
              do {
                int id = cursor.getInt(0);
                ContentValues values = new ContentValues();
                values.put("popular", 1);
                db.update("characters", values, "_id = " + id, null);
              } while (cursor.moveToNext());
            }
            cursor.close();
            cursor = null;
          }
        }
      }

    } catch (Exception e) {

    } finally {
      if (scanner != null) {
        scanner.close();
        scanner = null;
      }
      if (db != null) {
        db.close();
        db = null;
      }
    }
  }
}
