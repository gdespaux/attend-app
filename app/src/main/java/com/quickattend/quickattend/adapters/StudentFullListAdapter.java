package com.quickattend.quickattend.adapters;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.CircularNetworkImageView;

import java.util.ArrayList;
import java.util.HashMap;

public class StudentFullListAdapter extends BaseAdapter implements Filterable {
    Context context;
    ArrayList<HashMap<String, String>> list;
    ArrayList<HashMap<String, String>> mOriginalValues;
    String[] fromString;
    int[] toView;

    private static LayoutInflater inflater = null;
    ImageLoader imageLoader = AppController.getInstance().getImageLoader();

    public StudentFullListAdapter(Context thisContext, ArrayList<HashMap<String, String>> hashMaps, String[] fromThis, int[] toThat) {
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
            convertView = inflater.inflate(R.layout.student_list_full_item, null);
            holder = new ViewHolder();

            holder.studentID = (TextView) convertView.findViewById(R.id.studentID);
            holder.studentName = (TextView) convertView.findViewById(R.id.studentName);
            holder.studentPhoto = (CircularNetworkImageView) convertView.findViewById(R.id.studentPhoto);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.studentID.setText(list.get(position).get("studentID"));
        holder.studentName.setText(list.get(position).get("studentName"));
        holder.studentPhoto.setImageUrl(list.get(position).get("studentPhoto"), imageLoader);

        return convertView;
    }

    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                list = (ArrayList<HashMap<String, String>>) results.values; // has the filtered values
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                ArrayList<HashMap<String, String>> FilteredArrList = new ArrayList<HashMap<String, String>>();

                if (mOriginalValues == null) {
                    mOriginalValues = new ArrayList<HashMap<String, String>>(list); // saves the original data in mOriginalValues
                    Log.e("ORIGINAL SET", "SET");
                }

                /********
                 *
                 *  If constraint(CharSequence that is received) is null returns the mOriginalValues(Original) values
                 *  else does the Filtering and returns FilteredArrList(Filtered)
                 *
                 ********/
                if (constraint == null || constraint.length() == 0 || constraint.equals("backspaceNOW")) {
                    // set the Original result to return
                    results.count = mOriginalValues.size();
                    results.values = mOriginalValues;
                    Log.e("WORKING", mOriginalValues.toString());
                } else {
                    constraint = constraint.toString().toLowerCase();
                    for (int i = 0; i < mOriginalValues.size(); i++) {
                        String studentID = mOriginalValues.get(i).get("studentID");
                        String studentName = mOriginalValues.get(i).get("studentName");
                        String studentPhoto = mOriginalValues.get(i).get("studentPhoto");
                        if (studentName.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            HashMap<String, String> filteredStudent = new HashMap<>();
                            filteredStudent.put("studentName", studentName);
                            filteredStudent.put("studentID", studentID);
                            filteredStudent.put("studentPhoto", studentPhoto);
                            FilteredArrList.add(filteredStudent);
                        }
                    }

                    results.count = FilteredArrList.size();
                    results.values = FilteredArrList;
                    Log.e("VALUES", results.values.toString());
                    Log.e("CONSTRAINT", constraint.toString());
                }

                return results;
            }
        };

        return filter;
    }

    private static class ViewHolder {
        TextView studentID;
        TextView studentName;
        CircularNetworkImageView studentPhoto;
    }
}
