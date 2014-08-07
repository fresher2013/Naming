package com.miemie.naming;

import java.util.HashSet;
import java.util.logging.MemoryHandler;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class AbandonChars extends Activity {

  private HashSet<String> mAbandonList = new HashSet<String>();
  private EditText mEditText;
  private TextView mText;
  private Button mAdd,mRemove;
  private View.OnClickListener mClickListener = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      String txt = null;

      if (mEditText != null) {
        txt = mEditText.getEditableText().toString();
      }

      if (TextUtils.isEmpty(txt)) {
        return;
      }

      if (v.getId() == R.id.btn_add) {
        char[] chars = txt.toCharArray();
        for (char c : chars) {
          if (!mAbandonList.contains(String.valueOf(c))) {
            mAbandonList.add(String.valueOf(c));
            updateTxt();            
          }
          mEditText.setText("");
        }
      } else if (v.getId() == R.id.btn_remove) {
        char[] chars = txt.toCharArray();
        for (char c : chars) {
          if (mAbandonList.contains(String.valueOf(c))) {
            mAbandonList.remove(String.valueOf(c));
            updateTxt();
          }
        }
        mEditText.setText("");
      }
    }
  };

  Handler mHandler = new Handler(){
    public void dispatchMessage(android.os.Message msg) {
      if(msg.what==1){
        mRemove.setEnabled(true);
        mAdd.setEnabled(true);
        updateTxt();
      }
    };
  };
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);



    setContentView(R.layout.activity_abandon);

    mAdd = (Button) findViewById(R.id.btn_add);
    mAdd.setOnClickListener(mClickListener);
    mAdd.setEnabled(false);
    mRemove = (Button) findViewById(R.id.btn_remove);
    mRemove.setOnClickListener(mClickListener);
    mRemove.setEnabled(false);

    mEditText = (EditText) findViewById(R.id.editText1);
    mText = (TextView) findViewById(R.id.all);

    new Thread() {

      public void run() {
        mAbandonList = Utils.getAbandonList();
        mHandler.sendEmptyMessage(1);
      };

    }.start();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  public void finish() {
    super.finish();
    Utils.saveToAbandonFile(mAbandonList);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, 1, 0, "Clear all");
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == 1) {
      mAbandonList.clear();
      updateTxt();
      Utils.saveToAbandonFile(mAbandonList);
    }
    return super.onOptionsItemSelected(item);
  }

  private void updateTxt() {
    StringBuilder sb = new StringBuilder();
    for (String word : mAbandonList) {
      sb.append(word);
      sb.append(" ");
    }
    mText.setText(sb.toString());
  }
}