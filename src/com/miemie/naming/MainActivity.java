
package com.miemie.naming;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private SharedPreferences mPref;

    EditText mFamilyName;
    Spinner mSpinner;
    RelativeLayout mFilter1;
    RelativeLayout mFilter2;

    TextView mInfo1;
    TextView mInfo2;

    HashSet<String> mResult1;
    HashSet<String> mResult2;

    SQLiteDatabase mDB;

    ProgressDialog mDialog;
    PowerManager.WakeLock mWakeLock;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                if (mWakeLock != null) {
                    mWakeLock.release();
                    mWakeLock = null;
                }
                if (mDialog != null) {
                    mDialog.dismiss();
                    mDialog = null;
                    Intent it = new Intent(MainActivity.this, ResultActivity2.class);
                    it.putExtra("file", (String) msg.obj);
                    MainActivity.this.startActivity(it);
                }
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDB = Utils.openDatabase(this);

        mPref = getSharedPreferences(Constant.PREF_NAME, Context.MODE_PRIVATE);

        mFamilyName = (EditText) findViewById(R.id.family_name);
        mFamilyName.setText(Utils.getStringPrefValue(mPref, Constant.PREF_FAMILYNAME, 0));
        mSpinner = (Spinner) findViewById(R.id.character_no);

        int maxchars = Utils.getIntPrefValue(mPref, Constant.PREF_MAX_CHARS, 0);
        mSpinner.setSelection((maxchars > 0) ? (maxchars - 1) : 0);

        mInfo1 = (TextView) findViewById(R.id.text12);
        mInfo2 = (TextView) findViewById(R.id.text22);

        mFilter1 = (RelativeLayout) findViewById(R.id.name_filter1);
        mFilter1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent it = new Intent(MainActivity.this, CharFilter.class);
                it.putExtra("id", 1);
                MainActivity.this.startActivityForResult(it, 1);
            }
        });
        mFilter2 = (RelativeLayout) findViewById(R.id.name_filter2);
        mFilter2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent it = new Intent(MainActivity.this, CharFilter.class);
                it.putExtra("id", 2);
                MainActivity.this.startActivityForResult(it, 1);
            }
        });
        
        Button review1 = (Button) findViewById(R.id.review1);
        review1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent it = new Intent(MainActivity.this, CharReview2.class);
                it.putExtra("id", 1);
                MainActivity.this.startActivityForResult(it, 1);
            }
        });
        Button review2 = (Button) findViewById(R.id.review2);
        review2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent it = new Intent(MainActivity.this, CharReview2.class);
                it.putExtra("id", 2);
                MainActivity.this.startActivityForResult(it, 1);
            }
        });

        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (id == 0) {
                    Utils.saveIntPrefValue(mPref, Constant.PREF_MAX_CHARS, 1, 0);
                    mFilter2.setVisibility(View.GONE);
                } else if (id == 1) {
                    mFilter2.setVisibility(View.VISIBLE);
                    Utils.saveIntPrefValue(mPref, Constant.PREF_MAX_CHARS, 2, 0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (mResult1 == null) {
            String temp = Utils.getStringPrefValue(mPref, Constant.PREF_CHARS, 1);
            mResult1 = Utils.combineSToSet(temp, "@");
            if (mResult1 != null)
                mInfo1.setText(buildInfoString(mResult1.size()));
        }
        if (mResult2 == null) {
            String temp = Utils.getStringPrefValue(mPref, Constant.PREF_CHARS, 2);
            mResult2 = Utils.combineSToSet(temp, "@");
            if (mResult2 != null)
                mInfo2.setText(buildInfoString(mResult2.size()));
        }

        Button submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final String familyname = mFamilyName.getEditableText().toString();
                if (!TextUtils.isEmpty(familyname)) {
                    Utils.saveStringPrefValue(mPref, Constant.PREF_FAMILYNAME, familyname, 0);
                }

                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Naming");
                mWakeLock.acquire();

                mDialog = new ProgressDialog(MainActivity.this);
                mDialog.setCancelable(false);
                mDialog.show();

                new Thread() {

                    @Override
                    public void run() {
                        super.run();

                        StringBuilder sb1 = new StringBuilder();
                        sb1.append("result-");
                        sb1.append(new SimpleDateFormat("MMddHHmm").format(new Date(System
                                .currentTimeMillis())));
                        sb1.append(".txt");

                        File result = new File(Utils.getAppDir(), sb1.toString());
                        if (result.exists()) {
                            result.delete();
                        }
                        try {
                            result.createNewFile();
                        } catch (IOException e) {
                        }

                        if (mResult1 == null) {
                            String temp = Utils.getStringPrefValue(mPref, Constant.PREF_CHARS, 1);
                            mResult1 = Utils.combineSToSet(temp, "@");
                        }
                        if (mResult2 == null) {
                            String temp = Utils.getStringPrefValue(mPref, Constant.PREF_CHARS, 2);
                            mResult2 = Utils.combineSToSet(temp, "@");
                        }

                        FileWriter fw = null;

                        try {
                            fw = new FileWriter(result);

                            int size1 = mResult1.size();
                            int size2 = mResult2.size();
                            long size = (size1 * size2);

                            String[] chars1 = mResult1.toArray(new String[size1]);
                            String[] chars2 = mResult2.toArray(new String[size2]);

                            for (int i = 0; i < size; i++) {
                                String character1 = chars1[(int) (i % size1)];
                                String character2 = chars2[(int) (i % size2)];

                                if (!Utils.isGood(mDB, character1, character2)) {
                                    continue;
                                }

                                StringBuilder sb = new StringBuilder();
                                sb.append(familyname);
                                sb.append(character1);
                                sb.append(character2);
                                fw.write(sb.toString());
                                fw.write('\n');
//                                StringBuilder sb2 = new StringBuilder();
//                                sb2.append(familyname);
//                                sb2.append(character2);
//                                sb2.append(character1);
//                                fw.write(sb2.toString());
//                                fw.write('\n');
                            }
                            fw.flush();
                            fw.close();
                        } catch (FileNotFoundException e) {
                        } catch (IOException e) {
                        } finally {
                            if (fw != null)
                                try {
                                    fw.close();
                                } catch (IOException e) {
                                }
                        }
                        Message msg = mHandler.obtainMessage();
                        msg.what = 1;
                        msg.obj = result.getAbsolutePath();
                        mHandler.sendMessage(msg);
                    }

                }.start();

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDB != null)
            mDB.close();
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == 1 && data != null) {
                int id = data.getIntExtra("id", 0);
                if (id == 1) {
                    String temp = Utils.getStringPrefValue(mPref, Constant.PREF_CHARS, 1);
                    mResult1 = Utils.combineSToSet(temp, "@");
                    mInfo1.setText(buildInfoString(mResult1 != null ? mResult1.size() : 0));
                } else if (id == 2) {
                    String temp = Utils.getStringPrefValue(mPref, Constant.PREF_CHARS, 2);
                    mResult2 = Utils.combineSToSet(temp, "@");
                    mInfo2.setText(buildInfoString(mResult2 != null ? mResult2.size() : 0));
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.menu_item_updatedb).setEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_result) {

            Intent it = new Intent(MainActivity.this, ResultActivity2.class);
            MainActivity.this.startActivity(it);

            return true;
        } else if (item.getItemId() == R.id.menu_item_updatedb) {

            new Thread() {
                public void run() {
                    Utils.updateDB(MainActivity.this);
                };
            }.start();

        } else if (item.getItemId() == R.id.menu_item_abandonList) {

            Intent it = new Intent(MainActivity.this, AbandonChars.class);
            MainActivity.this.startActivity(it);

        }
        return super.onOptionsItemSelected(item);
    }

    private String buildInfoString(int size) {
        StringBuilder sb = new StringBuilder();
        sb.append("Select ");
        sb.append(size);
        sb.append(" Chars.");
        return sb.toString();
    }

}
