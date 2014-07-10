package com.miemie.naming;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Xml;


public class Utils {
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

}
