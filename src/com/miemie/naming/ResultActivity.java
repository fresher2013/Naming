package com.miemie.naming;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;

import com.shoushuo.android.tts.ITts;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class ResultActivity extends Activity implements View.OnClickListener{
  
  private final static String TAG = ResultActivity.class.getSimpleName();
  
  private String mCurrent;
  
  private TextView mName;
  private TextView mInfo1;
  private TextView mInfo2;
  
  
  private ImageButton mSpeak;
  private Button mNo1;
  private Button mNo2;
  private Button mNoBoth;
  private Button mLike;
  private Button mPass;
    
  private Scanner mScan;
  private FileOutputStream mFNewOut;
//  private FileOutputStream mFAbandonOut;
  
  private ITts ttsService;
  private boolean ttsBound;

  private SQLiteDatabase mDictDB;
  
  private HashSet<String> mAbandonList = new HashSet<String>();
  
  private ServiceConnection mConnection = new ServiceConnection() {

    @Override
    public void onServiceDisconnected(ComponentName name) {
      ttsService = null;
      ttsBound = false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      ttsService = ITts.Stub.asInterface(service);
      ttsBound = true;
      try {
        ttsService.initialize();
      } catch (Exception e) {
        Log.e(TAG, e.toString());
      }
    }
  };
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_result);

    new Thread(){

      @Override
      public void run() {
        super.run();
        mDictDB = Utils.openDictDatabase(ResultActivity.this);
      }
      
    }.start();
    
    Intent it = getIntent();

    final File dir = Utils.getAppDir();
    File input = new File(dir, "result.txt");

    if (it != null) {
      String filename = it.getStringExtra("file");
      if (!TextUtils.isEmpty(filename)) input = new File(filename);
    }

    if (!input.exists()) {
      Toast.makeText(this, "No result.txt found.", Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    
    File output = new File(dir, "new_"+input.getName());
    if (output.exists()) {
      output.delete();
    }

    new Thread(){
      
      public void run() {
        File abandon = new File(dir, "abandon.txt");
        Scanner scan;
        try {
          scan = new Scanner(abandon);
          if(scan!=null){
            while(scan.hasNextLine()){
              mAbandonList.add(scan.nextLine());
            }
          }
        } catch (FileNotFoundException e) {
        }

      };
      
    }.start();
    
    try {
      output.createNewFile();
      mScan = new Scanner(input);      
      mFNewOut = new FileOutputStream(output, false);
//      mFAbandonOut = new FileOutputStream(abandon, true);
    } catch (FileNotFoundException e) {} catch (IOException e) {}


    mCurrent = getNext();

    mName = (TextView) findViewById(R.id.name);

    mSpeak = (ImageButton) findViewById(R.id.speak);
    mSpeak.setTag(R.id.speak);
    mNo1 = (Button) findViewById(R.id.nomorechar1);
    mNo1.setTag(R.id.nomorechar1);
    mNo2 = (Button) findViewById(R.id.nomorechar2);
    mNo2.setTag(R.id.nomorechar2);
    mNoBoth = (Button) findViewById(R.id.nomore2char);
    mNoBoth.setTag(R.id.nomore2char);
    mLike = (Button) findViewById(R.id.yes);
    mLike.setTag(R.id.yes);
    mPass = (Button) findViewById(R.id.no);
    mPass.setTag(R.id.no);

    mSpeak.setOnClickListener(this);
    mNo1.setOnClickListener(this);
    mNo2.setOnClickListener(this);
    mNoBoth.setOnClickListener(this);
    mLike.setOnClickListener(this);
    mPass.setOnClickListener(this);

    mInfo1 = (TextView) findViewById(R.id.char1);
    mInfo2 = (TextView) findViewById(R.id.char2);
  }

  @Override
  protected void onResume() {
    super.onResume();
    updateView();
  }

  @Override
  public void onClick(View v) {
    int id = (int) v.getTag();
    if (id == R.id.speak) {
      if (!TextUtils.isEmpty(mCurrent)) {
        try {
          ttsService.speak(mCurrent, 1);
        } catch (RemoteException e) {}
      }
      return;
    } else if (id == R.id.nomore2char) {
      mAbandonList.add(getChar(mCurrent, 1));
      mAbandonList.add(getChar(mCurrent, 2));
    } else if (id == R.id.nomorechar1) {
      mAbandonList.add(getChar(mCurrent, 1));
    } else if (id == R.id.nomorechar2) {
      mAbandonList.add(getChar(mCurrent, 2));
    } else if (id == R.id.yes) {
      saveToFile(mFNewOut, mCurrent);
    }
    
    String temp = getNext();
    if (!TextUtils.isEmpty(temp)) {
      mCurrent = temp;
    }
    updateView();
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (!ttsBound) {
      bindService(new Intent("com.shoushuo.android.tts.intent.action.InvokeTts"), mConnection,
          Context.BIND_AUTO_CREATE);
    }
  }
  
  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (ttsBound) {
      ttsBound = false;
      unbindService(mConnection);
    }

    if (mDictDB != null) {
      mDictDB.close();
      mDictDB = null;
    }
    
    if (mScan != null) {
      mScan.close();
      mScan = null;
    }
    if (mFNewOut != null) {
      try {
        mFNewOut.close();
      } catch (IOException e) {}
      mFNewOut = null;
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_pinyin_filter, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.menu_item_ok) {
      
      saveToAbandonFile();
      
//      new Thread(){
//        
//        public void run() {
//          final File dir = Utils.getAppDir();
//          File temp = new File(dir, "result.txt.temp");
//          temp.createNewFile();
//          try {
//            FileWriter fw = new FileWriter(temp);
//            fw.write(str);
//          } catch (IOException e) {
//          }    
//          
//        };
//      }.start();
      
      finish();
      
      return true;
    }
    return super.onOptionsItemSelected(item);
  }  
  
  private String getNext() {
    if (mScan != null && mScan.hasNextLine()) {
      while (mScan.hasNextLine()) {
        String ret = mScan.nextLine();
        String name2 = getChar(ret, 2);
        String name1 = getChar(ret, 1);

        if (TextUtils.isEmpty(name1) || mAbandonList.contains(name1)
            || mAbandonList.contains(name2)) {
          continue;
        }

        Log.e(TAG, "getNext " + ret);
        return ret;
      }
    }

    return null;
  }
  
  private String getChar(String str, int index) {
    if (!TextUtils.isEmpty(str)) {
      char[] chars = str.toCharArray();
      if (index >= chars.length) {
        return null;
      }
      return new String(chars, index, 1);
    }
    return null;
  }
  
  private String getCharNote(String str) {
    if(TextUtils.isEmpty(str))
      return null;
    
    if (mDictDB != null) {
      Cursor c = mDictDB.rawQuery("select jijie from zi where zi = ?", new String[] {str});
      if (c != null) {
        if (c.getCount() > 0) {
          c.moveToFirst();
          String jijie = c.getString(0);
          StringBuilder sb = new StringBuilder();
          int index = jijie.indexOf("����");
          if (index == -1) {
            sb.append(jijie);
          } else {
            sb.append(jijie.substring(0, index));
          }
          return sb.toString();

        }
        c.close();
      }
    }
    return null;
  }
  
  private void updateView() {
    if (TextUtils.isEmpty(mCurrent)) return;

    if (mName != null) mName.setText(mCurrent);

    String name2 = getChar(mCurrent, 2);
    String name1 = getChar(mCurrent, 1);
        
    if (mNo1 != null) {
      StringBuilder sb = new StringBuilder();
//      sb.append("No more ");
      sb.append(name1);
      mNo1.setText(sb.toString());      
    }
    
    if (mInfo1 != null) {
      mInfo1.setText(getCharNote(name1));      
    }
    
    if (TextUtils.isEmpty(name2)) {
      mNo2.setVisibility(View.GONE);
      mNoBoth.setVisibility(View.GONE);
    } else {
      mNo2.setVisibility(View.VISIBLE);
      mNoBoth.setVisibility(View.VISIBLE);
      if (mNo2 != null) {
        StringBuilder sb = new StringBuilder();
//        sb.append("No more ");
        sb.append(name2);
        mNo2.setText(sb.toString());
      }

      if (mNoBoth != null) {
        StringBuilder sb = new StringBuilder();
//        sb.append("No more ");
        sb.append(name1);
        sb.append("&");
        sb.append(name2);
        mNoBoth.setText(sb.toString());
      }
      
      if (mInfo2 != null) {
        mInfo2.setText(getCharNote(name2));      
      }
    }
  }
  
  private void saveToFile(FileOutputStream fout, String character) {
    if (fout != null && !TextUtils.isEmpty(character)) {
      try {
        fout.write('\n');
        fout.write(character.getBytes());
        fout.flush();
      } catch (IOException e) {
      }
    }
  }

  private void saveToAbandonFile() {
    final File dir = Utils.getAppDir();
    File abandon = new File(dir, "abandon.txt");
    if (abandon.exists()) {
      abandon.delete();
    }
    FileOutputStream fout = null;
    try {
      abandon.createNewFile();
      fout = new FileOutputStream(abandon, true);
      for (String str : mAbandonList) {
        fout.write(str.getBytes());
        fout.write('\n');
      }
      fout.flush();
    } catch (FileNotFoundException e1) {

    } catch (IOException e2) {

    } finally {
      try {
        if (fout != null) fout.close();
      } catch (IOException e) {}
    }
  }
}
