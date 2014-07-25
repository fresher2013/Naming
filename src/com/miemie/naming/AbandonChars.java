package com.miemie.naming;

import java.util.HashSet;

import android.app.Activity;
import android.os.Bundle;

public class AbandonChars extends Activity {

  private HashSet<String> mAbandonList = new HashSet<String>();
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    new Thread(){
      
      public void run() {
        mAbandonList = Utils.getAbandonList();
        
      };
      
    }.start();
    
    setContentView(R.layout.activity_abandon);
  }
  
  @Override
  protected void onResume() {
    super.onResume();
  }
  
  @Override
  protected void onDestroy() {
    super.onDestroy();
    Utils.saveToAbandonFile(mAbandonList);
  }
}
