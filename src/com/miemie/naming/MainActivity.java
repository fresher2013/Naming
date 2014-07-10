package com.miemie.naming;

import com.shoushuo.android.tts.ITts;

import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;

public class MainActivity extends Activity{

  private final static String TAG = MainActivity.class.getSimpleName();

  private ITts ttsService;
  private boolean ttsBound;
  EditText mFamilyName;
  Spinner mSpinner;
  RelativeLayout mFilter1;
  RelativeLayout mFilter2;

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

    mFamilyName = (EditText) findViewById(R.id.family_name);
    mSpinner = (Spinner) findViewById(R.id.character_no);
    
    mFilter1 = (RelativeLayout) findViewById(R.id.name_filter1);
    mFilter1.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent it = new Intent(MainActivity.this, CharFilter.class);
        it.putExtra("id", 1);
        MainActivity.this.startActivity(it);
      }
    });
    mFilter2 = (RelativeLayout) findViewById(R.id.name_filter2);
    mFilter1.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent it = new Intent(MainActivity.this, CharFilter.class);
        it.putExtra("id", 2);
        MainActivity.this.startActivity(it);
      }
    });
        
    mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (id == 0) {
          mFilter2.setVisibility(View.GONE);
        } else if (id == 1) {
          mFilter2.setVisibility(View.VISIBLE);
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
 
      }});
    
    Button submit = (Button) findViewById(R.id.submit);
    submit.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {


      }
    }); 
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

}
