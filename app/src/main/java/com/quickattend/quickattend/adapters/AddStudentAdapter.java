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

import com.quickattend.quickattend.R;

import java.util.ArrayList;
import java.util.HashMap;

public class AddStudentAdapter extends BaseAdapter implements Filterable {
    Context context;
    ArrayList<HashMap<String, String>> list;
    String[] fromString;
    int[] toView;

    private static LayoutInflater inflater = null;

    public AddStudentAdapter(Context thisContext, ArrayList<HashMap<String, String>> hashMaps, String[] fromThis, int[] toThat) {
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
            convertView = inflater.inflate(R.layout.typeahead_student_list_item, null);
            holder = new ViewHolder();

            holder.studentID = (TextView) convertView.findViewById(R.id.studentID);
            holder.studentName = (TextView) convertView.findViewById(R.id.studentName);

            //Log.i("RESPONSE", list.toString());

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.studentID.setText(list.get(position).get("studentID"));
        holder.studentName.setText(list.get(position).get("studentName"));

        return convertView;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            ArrayList<HashMap<String, String>> mOriginalValues;

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint,FilterResults results) {

                list = (ArrayList<HashMap<String, String>>) results.values; // has the filtered values
                notifyDataSetChanged();  // notifies the data with new filtered values
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();        // Holds the results of a filtering operation in values
                ArrayList<HashMap<String, String>> FilteredArrList = new ArrayList<HashMap<String, String>>();

                if (mOriginalValues == null) {
                    mOriginalValues = new ArrayList<HashMap<String, String>>(list); // saves the original data in mOriginalValues
                }

                /********
                 *
                 *  If constraint(CharSequence that is received) is null returns the mOriginalValues(Original) values
                 *  else does the Filtering and returns FilteredArrList(Filtered)
                 *
                 ********/
                if (constraint == null || constraint.length() == 0) {

                    // set the Original result to return
                    results.count = mOriginalValues.size();
                    results.values = mOriginalValues;
                } else {
                    constraint = constraint.toString().toLowerCase();
                    for (int i = 0; i < mOriginalValues.size(); i++) {
                        String data = mOriginalValues.get(i).get("studentName");
                        String studentID = mOriginalValues.get(i).get("studentID");
                        String studentName = mOriginalValues.get(i).get("studentName");
                        if (data.toLowerCase().startsWith(constraint.toString())) {
                            HashMap<String,String> filteredStudent = new HashMap<>();
                            filteredStudent.put("studentID", studentID);
                            filteredStudent.put("studentName",studentName);
                            FilteredArrList.add(filteredStudent);
                        }
                    }
                    // set the Filtered result to return
                    results.count = FilteredArrList.size();
                    results.values = FilteredArrList;
                }
                return results;
            }
        };
        return filter;
    }

    private static class ViewHolder {
        TextView studentID;
        TextView studentName;

    }
}
