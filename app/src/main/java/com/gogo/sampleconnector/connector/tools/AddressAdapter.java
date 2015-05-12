package com.gogo.sampleconnector.connector.tools;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Adapter for address list.
 */
public class AddressAdapter extends BaseAdapter {

    ArrayList<String> addrs;
    Context context;
    int textSize = 30;
    int textColor = 0xffffffff;

    public AddressAdapter(ArrayList<String> addrs, Context context) {
        this.addrs = addrs;
        this.context = context;
    }

    public void setTextColor(int color) {
        textColor = color;
    }

    public void setTextSize(int size) {
        textSize = size;
    }

    @Override
    public int getCount() {
        return addrs.size();
    }

    @Override
    public Object getItem(int position) {
        return addrs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view = new TextView(context);
        view.setLayoutParams(new AbsListView.LayoutParams(
                AbsListView.LayoutParams.WRAP_CONTENT,
                AbsListView.LayoutParams.WRAP_CONTENT));
        view.setText(addrs.get(position));
        view.setTextColor(textColor);
        view.setTextSize(textSize);
        return view;
    }
}
