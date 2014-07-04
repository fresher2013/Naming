package com.miemie.naming;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;

import com.shoushuo.android.tts.ITts;

import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.os.Build;

public class MainActivity extends ActionBarActivity {

  private final static String TAG = MainActivity.class.getSimpleName();

  private ITts ttsService;
  private boolean ttsBound;
  EditText mEditText;

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
    setContentView(R.layout.activity_main);
    
    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment())
          .commit();
    }

    new Thread(){

      @Override
      public void run() {
        super.run();
        ChineseToPinyinResource pyRes = new ChineseToPinyinResource(getApplicationContext());

        HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();        
        Enumeration<Object> List = pyRes.getUnicodeToHanyuPinyinTable().keys();        
        while (List.hasMoreElements()) {
          String object = (String) List.nextElement();
          char c = Utils.HexString2Char(object);
          String cstr = String.valueOf(c);
          String[] outStrings = pyRes.getHanyuPinyinStringArray(c);

          if (outStrings != null) {
            for (String str : outStrings) {
              ArrayList<String> aList;
              if (map.containsKey(str)) {
                aList = map.get(str);
                aList.add(cstr);
              } else {
                aList = new ArrayList<String>();
                aList.add(cstr);
                map.put(str, aList);
              }
            }
          } else {
            Log.e(TAG, object + " " + c);
          }
        }
        for(String key : map.keySet()){
          Log.e(TAG, key+" "+map.get(key).size()+ " "+map.get(key).toString());
        }
      }
      
    }.start();  
    
    new Thread(){

      @Override
      public void run() {
        super.run();
        try {
          InputStream in = MainActivity.this.getAssets().open("Stroke.csv");
          Scanner scanner = new Scanner(in);
          PrintWriter out = new PrintWriter(MainActivity.this.openFileOutput("unicode_to_hanzi_bihua.txt",0));
          char count = 0x4e00;
          while (scanner.hasNextLine()) {
              String line = scanner.nextLine();
              StringBuilder sb = new StringBuilder(); 
              sb.append(Integer.toHexString(count));
              sb.append("(");
              sb.append(line);
              sb.append(")");
              count++;
              out.println(sb.toString());
          }
          out.flush();
          out.close();
          in.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }.start();
    
  }



  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (ttsBound) {
      ttsBound = false;
      unbindService(mConnection);
    }

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
  public boolean onCreateOptionsMenu(Menu menu) {

    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * A placeholder fragment containing a simple view.
   */
  @SuppressLint("ValidFragment")
  public class PlaceholderFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View rootView = inflater.inflate(R.layout.fragment_main, container, false);
      
      mEditText = (EditText) rootView.findViewById(R.id.input);

      Button btn = (Button) rootView.findViewById(R.id.speak);
      btn.setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View v) {

          String text = mEditText.getEditableText().toString();
          if (!TextUtils.isEmpty(text)) {

            try {
              ttsService.speak(text, TextToSpeech.QUEUE_ADD);
            } catch (RemoteException e) {

            }
          }

        }
      });      
      
      return rootView;
    }
  }

}
