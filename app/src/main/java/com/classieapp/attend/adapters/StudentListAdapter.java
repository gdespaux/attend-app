package com.classieapp.attend.adapters;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.classieapp.attend.R;

import java.util.ArrayList;
import java.util.HashMap;

public class StudentListAdapter extends BaseAdapter {
    Context context;
    ArrayList<HashMap<String,String>> list;
    String[] fromString;
    int[] toView;

    private static LayoutInflater inflater = null;

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
            holder.studentPresent = (CheckBox) convertView.findViewById(R.id.studentPresent);

            holder.studentID.setText(list.get(position).get("studentID"));
            holder.studentName.setText(list.get(position).get("studentName"));

            if(list.get(position).get("studentPresent").equals("yes")){
                holder.studentPresent.setChecked(true);
            } else {
                holder.studentPresent.setChecked(false);
            }

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView studentID;
        TextView studentName;
        CheckBox studentPresent;
    }
}
