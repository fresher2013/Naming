package com.miemie.naming;

import android.R.menu;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CharFilter extends Activity implements OnCheckedChangeListener{

  private EditText mMaxStroke;
  private EditText mMinStroke;
  private TextView mInfo;

  private int mID = 1;
  private int mTone;
  private String mPinyin;
  private int mPinyinSize;
  
  private SharedPreferences mPref;  
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_filter);

    Intent it = getIntent();
    if (it != null) {
      mID = it.getIntExtra("id", 1);
    }

    mPref = getSharedPreferences(Constant.PREF_NAME, Context.MODE_PRIVATE);

    mTone = Utils.getIntPrefValue(mPref, Constant.PREF_TONE, mID);
    mPinyin = Utils.getStringPrefValue(mPref, Constant.PREF_PINYIN, mID);
    if (!TextUtils.isEmpty(mPinyin)) {
      String[] strs = mPinyin.split("@");
      mPinyinSize = (strs != null ? strs.length : 0);
    }

    mMaxStroke = (EditText) findViewById(R.id.max_stroke);
    mMaxStroke.setText(String.valueOf(Utils.getIntPrefValue(mPref, Constant.PREF_MAX_STROKE, mID)));

    mMinStroke = (EditText) findViewById(R.id.min_stroke);
    mMinStroke.setText(String.valueOf(Utils.getIntPrefValue(mPref, Constant.PREF_MIN_STROKE, mID)));    
    
    mInfo = (TextView) findViewById(R.id.text2);
    mInfo.setText(buildInfoString(mPinyinSize));
    
    RelativeLayout rl = (RelativeLayout) findViewById(R.id.pinyin_filter);
    rl.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent it = new Intent(CharFilter.this, PinyinActivity.class);
        it.putExtra("pinyin", mPinyin);
        CharFilter.this.startActivityForResult(it, 1);
      }
    });

    CheckBox tone1 = (CheckBox) findViewById(R.id.tone1);
    tone1.setTag(R.id.tone1);
    tone1.setChecked((mTone & Constant.TONE_1) == Constant.TONE_1);
    tone1.setOnCheckedChangeListener(this);

    CheckBox tone2 = (CheckBox) findViewById(R.id.tone2);
    tone2.setTag(R.id.tone2);
    tone2.setChecked((mTone & Constant.TONE_2) == Constant.TONE_2);
    tone2.setOnCheckedChangeListener(this);

    CheckBox tone3 = (CheckBox) findViewById(R.id.tone3);
    tone3.setTag(R.id.tone3);
    tone3.setChecked((mTone & Constant.TONE_3) == Constant.TONE_3);
    tone3.setOnCheckedChangeListener(this);

    CheckBox tone4 = (CheckBox) findViewById(R.id.tone4);
    tone4.setTag(R.id.tone4);
    tone4.setChecked((mTone & Constant.TONE_4) == Constant.TONE_4);
    tone4.setOnCheckedChangeListener(this);


  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (resultCode == RESULT_OK) {
      if (requestCode == 1 && data != null) {
        mPinyinSize = data.getIntExtra("size", mPinyinSize);
        mPinyin = data.getStringExtra("pinyin");
        mInfo.setText(buildInfoString(mPinyinSize));
      }
    }
    
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (buttonView instanceof CheckBox) {
      int id = (int) buttonView.getTag();
      int filter = Constant.TONE_1;
      if (id == R.id.tone1) {
        filter = Constant.TONE_1;
      } else if (id == R.id.tone2) {
        filter = Constant.TONE_2;
      } else if (id == R.id.tone3) {
        filter = Constant.TONE_3;
      } else if (id == R.id.tone4) {
        filter = Constant.TONE_4;
      } else {
        return;
      }

      if (isChecked) {
        mTone |= filter;
      } else {
        mTone &= (~filter);
      }

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
      String value = mMaxStroke.getEditableText().toString();
      int strokes = 0;
      if (!TextUtils.isEmpty(value) && TextUtils.isDigitsOnly(value)) {
        strokes = Integer.parseInt(value);
      }
      Utils.saveIntPrefValue(mPref, Constant.PREF_MAX_STROKE, strokes, mID);
      
      strokes = 0;
      value = mMinStroke.getEditableText().toString();
      if (!TextUtils.isEmpty(value) && TextUtils.isDigitsOnly(value)) {
        strokes = Integer.parseInt(value);
      }      
      Utils.saveIntPrefValue(mPref, Constant.PREF_MIN_STROKE, strokes, mID);
      Utils.saveIntPrefValue(mPref, Constant.PREF_TONE, mTone, mID);
      Utils.saveStringPrefValue(mPref, Constant.PREF_PINYIN, mPinyin, mID);
      
      Intent it = new Intent();
      it.putExtra("id", mID);
      setResult(RESULT_OK, it);
      CharFilter.this.finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
  
  private String buildInfoString(int size){
    StringBuilder sb = new StringBuilder();
    sb.append("Select ");
    sb.append(size);
    sb.append(" Pinyins.");
    return sb.toString();
  }
}
