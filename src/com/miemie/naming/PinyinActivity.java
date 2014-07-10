package com.miemie.naming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class PinyinActivity extends Activity implements OnCheckedChangeListener,View.OnClickListener{

  private LayoutInflater mFactory;
  private ListView mList;
  private PinyinAdapter mAdapter;
    
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
        c.close();
      } else {
        mAllPinyins = null;
      }
      refreshSelected();
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
              mAdapter.refreshSelected();
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
    
    public void refreshSelected() {

      mDisplayedPinyins.clear();

      mSelectedPinyins = mUserSelectedPinyins.toArray(new String[mUserSelectedPinyins.size()]);
      Arrays.sort(mSelectedPinyins);

      ArrayList<String> sections = new ArrayList<String>();
      ArrayList<Integer> positions = new ArrayList<Integer>();
      // Create section indexer and add headers to the pinyin list
      String val = null;
      int count = 0;

      if (mSelectedPinyins.length > 0) {
        sections.add(mSelectedPinyinsHeaderString);
        positions.add(count);
        mDisplayedPinyins.add(mSelectedPinyinsHeaderString);
        count++;
        for (String str : mSelectedPinyins) {
          mDisplayedPinyins.add(str);
          count++;
        }
        mSelectedEndPosition = count;
      }

      for (String pinyin : mAllPinyins) {
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
    mAdapter.refreshSelected();
  }
  
}
