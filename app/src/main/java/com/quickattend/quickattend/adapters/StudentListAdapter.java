package com.quickattend.quickattend.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.CircularNetworkImageView;

import java.util.ArrayList;
import java.util.HashMap;

public class StudentListAdapter extends BaseAdapter {
    Context context;
    ArrayList<HashMap<String,String>> list;
    String[] fromString;
    int[] toView;

    private static LayoutInflater inflater = null;
    ImageLoader imageLoader = AppController.getInstance().getImageLoader();

    public StudentListAdapter(Context thisContext, ArrayList<HashMap<String,String>> hashMaps, String[] fromThis, int[] toThat) {
        // TODO Auto-generated constructor stub
        context = thisContext;
        list = hashMaps;
        fromString = fromThis;
        toView = toThat;

        inflater = (LayoutInflater) context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return list.size();
    }

    @Override
    public int getViewTypeCount() {
        int count = getCount();

        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.student_list_item, null);
            holder = new ViewHolder();

            holder.studentID = (TextView) convertView.findViewById(R.id.studentID);
            holder.studentName = (TextView) convertView.findViewById(R.id.studentName);
            holder.studentPhoto = (CircularNetworkImageView) convertView.findViewById(R.id.studentPhoto);
            holder.studentPresent = (CheckBox) convertView.findViewById(R.id.studentPresent);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.studentID.setText(list.get(position).get("studentID"));
        holder.studentName.setText(list.get(position).get("studentName"));
        holder.studentPhoto.setImageUrl(list.get(position).get("studentPhoto"), imageLoader);

        if(list.get(position).get("studentPresent").equals("yes")){
            holder.studentPresent.setChecked(true);
        } else {
            holder.studentPresent.setChecked(false);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView studentID;
        TextView studentName;
        CircularNetworkImageView studentPhoto;
        CheckBox studentPresent;
    }
}
