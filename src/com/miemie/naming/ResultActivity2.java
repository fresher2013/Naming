
package com.miemie.naming;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

public class ResultActivity2 extends Activity implements OnClickListener {

    private final static String TAG = ResultActivity2.class.getSimpleName();

    private final int COUNT = 14;

    TextView[] mTVs = new TextView[COUNT];
    private HashSet<String> mAbandonList = new HashSet<String>();
    private HashSet<String> mCharList = new HashSet<String>();
    private ArrayList<String> mNameList = new ArrayList<String>();
    private HashSet<String> mSelected = new HashSet<String>();

    private Scanner mScan;
    private FileWriter mFNewOut;

    ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_result2);

        TableRow row1 = (TableRow) findViewById(R.id.row1);
        mTVs[0] = (TextView) row1.findViewById(R.id.tv_1);
        mTVs[1] = (TextView) row1.findViewById(R.id.tv_2);
        TableRow row2 = (TableRow) findViewById(R.id.row2);
        mTVs[2] = (TextView) row2.findViewById(R.id.tv_1);
        mTVs[3] = (TextView) row2.findViewById(R.id.tv_2);
        TableRow row3 = (TableRow) findViewById(R.id.row3);
        mTVs[4] = (TextView) row3.findViewById(R.id.tv_1);
        mTVs[5] = (TextView) row3.findViewById(R.id.tv_2);
        TableRow row4 = (TableRow) findViewById(R.id.row4);
        mTVs[6] = (TextView) row4.findViewById(R.id.tv_1);
        mTVs[7] = (TextView) row4.findViewById(R.id.tv_2);
        TableRow row5 = (TableRow) findViewById(R.id.row5);
        mTVs[8] = (TextView) row5.findViewById(R.id.tv_1);
        mTVs[9] = (TextView) row5.findViewById(R.id.tv_2);
        TableRow row6 = (TableRow) findViewById(R.id.row6);
        mTVs[10] = (TextView) row6.findViewById(R.id.tv_1);
        mTVs[11] = (TextView) row6.findViewById(R.id.tv_2);
        TableRow row7 = (TableRow) findViewById(R.id.row7);
        mTVs[12] = (TextView) row7.findViewById(R.id.tv_1);
        mTVs[13] = (TextView) row7.findViewById(R.id.tv_2);

        Button next = (Button) findViewById(R.id.yes);
        next.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                saveToFile(mFNewOut, mSelected);
                getNext();
                updateView();
            }
        });

        for (int i = 0; i < mTVs.length; i++) {
            mTVs[i].setOnClickListener(this);
        }

        Intent it = getIntent();

        final File dir = Utils.getAppDir();
        File input = new File(dir, "result.txt");

        if (it != null) {
            String filename = it.getStringExtra("file");
            if (!TextUtils.isEmpty(filename))
                input = new File(filename);
        }

        if (!input.exists()) {
            Toast.makeText(this, "No result.txt found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        File output = new File(dir, "new_" + input.getName());
        if (output.exists()) {
            output.delete();
        }

        new Thread() {

            public void run() {
                HashSet<String> temp = Utils.getAbandonList(ResultActivity2.this);
                temp.addAll(mAbandonList);
                mAbandonList = temp;
            };

        }.start();

        try {
            output.createNewFile();
            mScan = new Scanner(input);
            mFNewOut = new FileWriter(output, true);
            // mFAbandonOut = new FileOutputStream(abandon, true);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        getNext();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mScan != null) {
            mScan.close();
            mScan = null;
        }
        if (mFNewOut != null) {
            try {
                mFNewOut.close();
            } catch (IOException e) {
            }
            mFNewOut = null;
        }
    }

    @Override
    public void onClick(View v) {
        Boolean state = (Boolean) v.getTag();

        v.setTag(!state);
        TextView tv = (TextView) v;
        if (state) {
            v.setBackgroundResource(android.R.color.background_light);
            mSelected.add(tv.getText().toString());
        } else {
            v.setBackgroundResource(android.R.color.holo_blue_dark);
            mSelected.remove(tv.getText().toString());
        }

    }

    private boolean getNext() {
        
        for (int i = 0; i < mTVs.length; i++) {
            mTVs[i].setTag(Boolean.FALSE);
        }
        
        mCharList.clear();
        mNameList.clear();
        mSelected.clear();

        if (mScan != null) {
            while (mScan.hasNextLine()) {
                String line = mScan.nextLine();
                String name2 = getChar(line, 2);
                String name1 = getChar(line, 1);

                if (TextUtils.isEmpty(name1) || mAbandonList.contains(name1)
                        || mAbandonList.contains(name2)) {
                    continue;
                }

                if (!mCharList.contains(name1)) {
                    mCharList.add(name1);
                }

                if (!mCharList.contains(name2)) {
                    mCharList.add(name2);
                }

                mNameList.add(line);
                if (mNameList.size() == COUNT) {
                    return true;
                }
            }
        }

        return true;
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

    private void updateView() {
        int i = 0;
        for (String str : mNameList) {
            mTVs[i].setText(str);
            i++;
        }
    }

    private void saveToFile(FileWriter fout, HashSet<String> toSave) {
        if (fout != null) {
            try {
                for (String str : toSave) {
                    fout.write(str);
                    fout.write('\n');
                }
                fout.flush();
            } catch (IOException e) {
            }
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                ResultActivity2.this.finish();
            }
        }

    };

    private boolean quit() {
        mDialog = new ProgressDialog(ResultActivity2.this);
        mDialog.setCancelable(false);
        mDialog.show();

        new Thread() {

            public void run() {
                final File dir = Utils.getAppDir();
                File temp = new File(dir, "result.txt.temp");
                if (temp.exists()) {
                    temp.delete();
                }
                FileWriter fw = null;
                try {
                    fw = new FileWriter(temp);
                    temp.createNewFile();
                    while (mScan.hasNextLine()) {
                        fw.write(mScan.nextLine());
                        fw.write("\n");
                    }

                    fw.flush();
                    fw.close();
                    fw = null;

                    File result = new File(dir, "result.txt");
                    if (result.exists()) {
                        result.delete();
                        result = null;
                    }

                    temp.renameTo(new File(dir, "result.txt"));

                } catch (Exception e) {
                } finally {
                    if (fw != null) {
                        try {
                            fw.close();
                        } catch (IOException e) {
                        }
                        fw = null;
                    }
                }

                mHandler.sendEmptyMessage(1);

            };
        }.start();

        return true;

    }
}
