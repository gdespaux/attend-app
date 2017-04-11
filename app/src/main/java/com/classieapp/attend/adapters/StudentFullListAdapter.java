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

import com.android.volley.toolbox.ImageLoader;
import com.classieapp.attend.R;
import com.classieapp.attend.app.AppController;
import com.classieapp.attend.utils.CircularNetworkImageView;

import java.util.ArrayList;
import java.util.HashMap;

public class StudentFullListAdapter extends BaseAdapter {
    Context context;
    ArrayList<HashMap<String,String>> list;
    String[] fromString;
    int[] toView;

    private static LayoutInflater inflater = null;
    ImageLoader imageLoader = AppController.getInstance().getImageLoader();

    public StudentFullListAdapter(Context thisContext, ArrayList<HashMap<String,String>> hashMaps, String[] fromThis, int[] toThat) {
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
        return getCount();
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
            convertView = inflater.inflate(R.layout.student_list_full_item, null);
            holder = new ViewHolder();

            holder.studentID = (TextView) convertView.findViewById(R.id.studentID);
            holder.studentName = (TextView) convertView.findViewById(R.id.studentName);
            holder.studentPhoto = (CircularNetworkImageView) convertView.findViewById(R.id.studentPhoto);

            holder.studentID.setText(list.get(position).get("studentID"));
            holder.studentName.setText(list.get(position).get("studentName"));
            holder.studentPhoto.setImageUrl(list.get(position).get("studentPhoto"), imageLoader);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView studentID;
        TextView studentName;
        CircularNetworkImageView studentPhoto;
    }
}
