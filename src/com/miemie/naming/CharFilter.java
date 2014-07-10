package com.miemie.naming;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;

public class CharFilter extends Activity{

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_filter);

    RelativeLayout rl = (RelativeLayout) findViewById(R.id.pinyin_filter);
    rl.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent it = new Intent(CharFilter.this, PinyinActivity.class);
        CharFilter.this.startActivity(it);
      }
    });
  }

}
