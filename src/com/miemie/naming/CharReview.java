package com.miemie.naming;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.HashSet;

public class CharReview extends Activity  implements OnClickListener {    
    private final int COLUMNS = 3;
    
    final int[] mIds = {
            R.id.row1,
            R.id.row2,
            R.id.row3,
            R.id.row4,
            R.id.row5,
            R.id.row6,
            R.id.row7,
    };
    
    private final int COUNT = mIds.length*COLUMNS;
    TextView[] mTVs = new TextView[COUNT];
    
    private int mID = 1;
    private SharedPreferences mPref;
    HashSet<String> mResult;
    String[] mSource;
    int mIndex = 0;
    ProgressDialog mDialog;
    
    SQLiteDatabase mDB;
    
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
          super.handleMessage(msg);
          if (msg.what == 1) {
            Intent it = new Intent();
            it.putExtra("id", mID);
            setResult(RESULT_OK, it);
            CharReview.this.finish();
          }
        }

      };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        mDB = Utils.openDatabase(this);
        
        Intent it = getIntent();
        if (it != null) {
            mID = it.getIntExtra("id", 1);
        }

        mPref = getSharedPreferences(Constant.PREF_NAME, Context.MODE_PRIVATE);
        String temp = Utils.getStringPrefValue(mPref, Constant.PREF_CHARS, mID);
        mResult = Utils.combineSToSet(temp, "@");
        mSource = temp.split("@");
        
        for (int i = 0; i < mIds.length; i++) {
            TableRow row = (TableRow) findViewById(mIds[i]);
            mTVs[i * COLUMNS + 0] = (TextView) row.findViewById(R.id.tv_1);
            mTVs[i * COLUMNS + 1] = (TextView) row.findViewById(R.id.tv_2);
            mTVs[i * COLUMNS + 2] = (TextView) row.findViewById(R.id.tv_3);
        }

        for (int i = 0; i < mTVs.length; i++) {
            mTVs[i].setOnClickListener(this);
            mTVs[i].setTag(Boolean.FALSE);
        }

        Button next = (Button) findViewById(R.id.yes);
        next.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                getNext();
                updateView();
            }
        });

    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateView();
    }
    
    @Override
    public void onClick(View v) {
        Boolean state = (Boolean) v.getTag();

        v.setTag(!state);
        TextView tv = (TextView) v;
        if (state) {
            v.setBackgroundResource(android.R.color.background_light);
            String str = tv.getText().toString();
            if (!mResult.contains(str)) {
                mResult.add(str);
            }
        } else {
            v.setBackgroundResource(android.R.color.holo_blue_dark);
            String str = tv.getText().toString();
            if (mResult.contains(str)) {
                mResult.remove(str);
            }
        }

    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        if (mDB != null) {
            mDB.close();
            mDB = null;
        }
    }
    
    @Override
    public void onBackPressed() {
        mDialog = new ProgressDialog(CharReview.this);
        mDialog.setCancelable(false);
        mDialog.show();

        new Thread() {
            public void run() {
                String resultString = Utils.allInOne(mResult.toArray(new String[mResult.size()]),
                        "@");
                Utils.saveStringPrefValue(mPref, Constant.PREF_CHARS, resultString, mID);

                Utils.saveToFile("charlist" + mID, mResult);

                mHandler.sendEmptyMessage(1);
            };
        }.start();
    }
    
    private void updateView() {
        SQLiteDatabase db = mDB;
        for (int i = 0; i < mTVs.length; i++) {
            int index = mIndex * mTVs.length + i;
            if (index < mSource.length) {
                String str = mSource[index];
                StringBuilder sb = new StringBuilder(mSource[index]);
                Cursor c = db.rawQuery("select pinyin,strokes from characters where hanzi=?",
                        new String[] {
                            str
                        });
                if (c == null) {
                    continue;
                }
                c.moveToFirst();
                sb.append("(");
                sb.append(c.getString(0));
                sb.append(",");
                sb.append(c.getInt(1));
                sb.append(")");
                mTVs[i].setText(sb.toString());
                c.close();
            } else {
                break;
            }
        }
    }
    
    private boolean getNext() {

        for (int i = 0; i < mTVs.length; i++) {
            mTVs[i].setTag(Boolean.FALSE);
            mTVs[i].setBackgroundResource(android.R.color.background_light);
        }

        mIndex++;
        return true;
    }
}
