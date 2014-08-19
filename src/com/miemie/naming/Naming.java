package com.miemie.naming;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Naming extends Application{

  private static final String TAG = Naming.class.getSimpleName();
  
  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate");
    new Thread() {
      public void run() {
        SQLiteDatabase db = Utils.openDictDatabase(Naming.this);
        db.close();
      };
    }.start();
    new Thread() {
      public void run() {
        SQLiteDatabase db = Utils.openDatabase(Naming.this);
        db.close();
      };
    }.start();
  }

}
