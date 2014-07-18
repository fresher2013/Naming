package com.miemie.naming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class PinyinActivity extends Activity implements OnCheckedChangeListener,View.OnClickListener{

  private LayoutInflater mFactory;
  private ListView mList;
  private PinyinAdapter mAdapter;
  
  private boolean bNoN = false;
  private boolean bNoR = false;
  private boolean bNoZhChSh = false;
  private boolean bNoBackNasals = false;
  
    
  private HashSet<String> mUserSelectedPinyins = new HashSet<String>();
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.activity_pinyin);
    
    mFactory = LayoutInflater.from(this);
        
    mAdapter = new PinyinAdapter(this);
    
    mList = (ListView) findViewById(R.id.pinyin_list);
    mList.setAdapter(mAdapter);
    mList.setFastScrollEnabled(true);
    
    Intent it = getIntent();
    if (it != null) {
      String temp = it.getStringExtra("pinyin");
      String[] strs = temp.split("@");
      for(String str:strs){
        mUserSelectedPinyins.add(str);
      }
    }    
    
    ActionBar actionBar = getActionBar();
    if (actionBar != null) {
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
    }
    
    Button filter = (Button) findViewById(R.id.filter);
    filter.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        showDialog(1);
      }
    });
    
    Button all = (Button) findViewById(R.id.all);
    all.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        mAdapter.selectAll();
      }
    });    
  }

  
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_pinyin_filter, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.menu_item_ok) {
      int size = mUserSelectedPinyins.size();
      if (size > 0) {
        String[] selected = mUserSelectedPinyins.toArray(new String[size]);
        StringBuilder sb = new StringBuilder();
        for (String pinyin : selected) {
          sb.append(pinyin);
          sb.append("@");
        }
        Intent it = new Intent();
        String ret = sb.toString();
        if(ret.endsWith("@")){
          ret = ret.substring(0, (ret.length()-2));
        }
        it.putExtra("pinyin", ret);
        it.putExtra("size", size);
        setResult(RESULT_OK, it);
        PinyinActivity.this.finish();
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  @Deprecated
  protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
    if (id == 1) {
      CheckBox checkBox1 = (CheckBox) dialog.findViewById(R.id.checkBox1);
      checkBox1.setChecked(bNoR);
      CheckBox checkBox2 = (CheckBox) dialog.findViewById(R.id.checkBox2);
      checkBox2.setChecked(bNoN);
      CheckBox checkBox3 = (CheckBox) dialog.findViewById(R.id.checkBox3);
      checkBox3.setChecked(bNoZhChSh);
      CheckBox checkBox4 = (CheckBox) dialog.findViewById(R.id.checkBox4);
      checkBox4.setChecked(bNoBackNasals);
    }
    super.onPrepareDialog(id, dialog, args);
  }


  @Override
  @Deprecated
  protected Dialog onCreateDialog(int id) {

    if (id == 1) {
      View view = LayoutInflater.from(this).inflate(R.layout.dialog_pinyin_filter, null);
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.PreferPinyin);
      builder.setView(view);
      final CheckBox check1 = (CheckBox) view.findViewById(R.id.checkBox1);
      check1.setChecked(bNoR);
      check1.setOnCheckedChangeListener(new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          bNoR = isChecked;
        }
      });
      final CheckBox check2 = (CheckBox) view.findViewById(R.id.checkBox2);
      check2.setChecked(bNoN);
      check2.setOnCheckedChangeListener(new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          bNoN = isChecked;
        }
      });
      final CheckBox check3 = (CheckBox) view.findViewById(R.id.checkBox3);
      check3.setChecked(bNoZhChSh);
      check3.setOnCheckedChangeListener(new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          bNoZhChSh = isChecked;
        }
      });
      final CheckBox check4 = (CheckBox) view.findViewById(R.id.checkBox4);
      check4.setChecked(bNoBackNasals);
      check4.setOnCheckedChangeListener(new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          bNoBackNasals = isChecked;
        }
      });

      builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {          
          mAdapter.refreshDataSet();
        }
      });
      return builder.create();
    }

    return super.onCreateDialog(id);
  }

  private class PinyinAdapter extends BaseAdapter implements SectionIndexer {
    private ArrayList<String> mDisplayedPinyins = new ArrayList<String>();
    private final String[] mAllPinyins;
    private Object [] mSectionHeaders;
    private Object [] mSectionPositions;
    private String[] mSelectedPinyins;
    
    private int mSelectedEndPosition = 0;
    
    private final String mSelectedPinyinsHeaderString;
    
    public PinyinAdapter(Context context) {
      super();

      mSelectedPinyinsHeaderString = context.getString(R.string.SelectedPinyinsLabel);

      SQLiteDatabase db = Utils.openDatabase(context);
      Cursor c = db.rawQuery("select * from pinyin", null);

      if (c != null) {
        ArrayList<String> pinyins = new ArrayList<String>();
        if (c.getCount() > 0) {
          c.moveToFirst();
          do {
            pinyins.add(c.getString(1));
          } while (c.moveToNext());
        }
        mAllPinyins = pinyins.toArray(new String[pinyins.size()]);
        Arrays.sort(mAllPinyins);
        c.close();
      } else {
        mAllPinyins = null;
      }
      
      db.close();
      refreshDataSet();
    }

    @Override
    public int getCount() {
      return (mDisplayedPinyins != null) ? mDisplayedPinyins.size() : 0;
    }

    @Override
    public Object getItem(int p) {
      if (mDisplayedPinyins != null && p >=0 && p < mDisplayedPinyins.size()) {
        return mDisplayedPinyins.get(p);
    }
    return null;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    private class ViewHolder {
      TextView pinyin;
      CheckBox selected;
      ImageView remove;
  }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (mDisplayedPinyins == null || position < 0 || position >= mDisplayedPinyins.size()) {
        return null;
      }
      View view = convertView;

      String pinyin = mDisplayedPinyins.get(position);
      if (pinyin.startsWith("HEADER") || pinyin.equals(mSelectedPinyinsHeaderString)) {
        if (view == null || view.findViewById(R.id.header) == null) {
          view = mFactory.inflate(R.layout.pinyin_list_header, parent, false);
        }

        TextView tv = (TextView) view.findViewById(R.id.header);
        if (pinyin.startsWith("HEADER")) {
          tv.setText(pinyin.substring(pinyin.length() - 1, pinyin.length()));
        } else {
          tv.setText(pinyin);
        }

      } else {
        if (view == null || view.findViewById(R.id.pinyin) == null) {
          view = mFactory.inflate(R.layout.pinyin_list_item, parent, false);
          final ViewHolder holder = new ViewHolder();
          holder.pinyin = (TextView) view.findViewById(R.id.pinyin);
          holder.selected = (CheckBox) view.findViewById(R.id.pinyin_onoff);
          holder.remove = (ImageView) view.findViewById(R.id.pinyin_remove);
          holder.remove.setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View view) {
              CompoundButton b = holder.selected;
              onCheckedChanged(b, false);
              b.setChecked(false);
              mAdapter.refreshDataSet();
            }
          });
          view.setTag(holder);
        }
        view.setOnClickListener(PinyinActivity.this);

        ViewHolder holder = (ViewHolder) view.getTag();

        if (position < mSelectedEndPosition) {
          holder.selected.setEnabled(false);
          holder.selected.setOnCheckedChangeListener(null);
          holder.remove.setVisibility(View.VISIBLE);
          view.setEnabled(false);
        } else {
          holder.selected.setVisibility(View.VISIBLE);
          holder.remove.setVisibility(View.GONE);
          holder.selected.setOnCheckedChangeListener(PinyinActivity.this);
          view.setEnabled(true);
        }

        holder.pinyin.setText(pinyin);
        holder.selected.setTag(pinyin);
        holder.selected.setChecked(mUserSelectedPinyins.contains(pinyin));
      }

      return view;
    }
    
    public void selectAll() {
      mUserSelectedPinyins.clear();
      for (String pinyin : mAllPinyins) {
        mUserSelectedPinyins.add(pinyin);
      }
      refreshDataSet();
    }
    
    public void refreshDataSet() {

      mDisplayedPinyins.clear();
      
//      mSelectedPinyins = mUserSelectedPinyins.toArray(new String[mUserSelectedPinyins.size()]);
//      Arrays.sort(mSelectedPinyins);

      ArrayList<String> sections = new ArrayList<String>();
      ArrayList<Integer> positions = new ArrayList<Integer>();
      // Create section indexer and add headers to the pinyin list
      String val = null;
      int count = 0;

      if (mSelectedPinyins!=null && mSelectedPinyins.length > 0) {
        sections.add("*");
        positions.add(count);
        mDisplayedPinyins.add(mSelectedPinyinsHeaderString);
        count++;
        for (String str : mSelectedPinyins) {
          if(filter(str)){
            mUserSelectedPinyins.remove(str);
            continue;
          }
          mDisplayedPinyins.add(str);
          count++;
        }
        mSelectedEndPosition = count;
      }

      for (String pinyin : mAllPinyins) {
        if (filter(pinyin)) {
          Log.e("TEST", "refreshDataSet "+pinyin);
          continue;
        }
        
        if (!pinyin.substring(0, 1).equals(val)) {
          val = pinyin.substring(0, 1);
          sections.add((new String(val)).toUpperCase());
          positions.add(count);
          // Add a header
          mDisplayedPinyins.add("HEADER" + val);
          count++;
        }

        mDisplayedPinyins.add(pinyin);
        count++;
      }

      mSectionHeaders = sections.toArray();
      mSectionPositions = positions.toArray();
      
      notifyDataSetChanged();
    }

    
    @Override
    public Object[] getSections() {
      return mSectionHeaders;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
      return (mSectionPositions != null) ? (Integer) mSectionPositions[sectionIndex] : 0;
    }

    @Override
    public int getSectionForPosition(int p) {
      if (mSectionPositions != null) {
        for (int i = 0; i < mSectionPositions.length - 1; i++) {
          if (p >= (Integer) mSectionPositions[i] && p < (Integer) mSectionPositions[i + 1]) {
            return i;
          }
        }
        if (p >= (Integer) mSectionPositions[mSectionPositions.length - 1]) {
          return mSectionPositions.length - 1;
        }
      }
      return 0;
    }
    
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    String pinyin = (String) buttonView.getTag();
    if (TextUtils.isEmpty(pinyin)) {
      return;
    }
    if (isChecked) {
      mUserSelectedPinyins.add(pinyin);
    } else {
      mUserSelectedPinyins.remove(pinyin);
    }
  }

  @Override
  public void onClick(View v) {
    CompoundButton b = (CompoundButton) v.findViewById(R.id.pinyin_onoff);
    boolean checked = b.isChecked();
    onCheckedChanged(b, checked);
    b.setChecked(!checked);
    mAdapter.refreshDataSet();
  }
  
  private boolean filter(String pinyin) {
    if (TextUtils.isEmpty(pinyin)) return false;

    String temp = pinyin.toLowerCase();
    if (bNoBackNasals) {
      if (temp.endsWith("ng")) {
        return true;
      }
    }
    if (bNoN) {
      if (temp.startsWith("n")) {
        return true;
      }
    }
    if (bNoR) {
      if (temp.startsWith("r")) {
        return true;
      }
    }
    if (bNoZhChSh) {
      if (temp.startsWith("zh") || temp.startsWith("ch") || temp.startsWith("sh")) {
        return true;
      }
    }
    return false;
  }
}
