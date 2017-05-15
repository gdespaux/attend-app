package com.quickattend.quickattend.adapters;


import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.quickattend.quickattend.R;
import com.quickattend.quickattend.activity.ClassStudentListActivity;
import com.quickattend.quickattend.activity.StudentListFragment;
import com.quickattend.quickattend.models.Student;
import com.quickattend.quickattend.models.StudentFull;
import com.quickattend.quickattend.models.StudentFullHolder;
import com.quickattend.quickattend.models.StudentHolder;
import com.quickattend.quickattend.utils.CircularNetworkImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StudentFullListAdapter extends RecyclerView.Adapter<StudentFullHolder> implements Filterable {

    private List<StudentFull> students;
    private List<StudentFull> mOriginalValues;
    private Context context;
    private int itemResource;

    // Allows to remember the last item shown on screen
    private int lastPosition = -1;

    public StudentFullListAdapter(Context context, int itemResource, List<StudentFull> students) {

        this.students = students;
        this.context = context;
        this.itemResource = itemResource;
    }

    @Override
    public StudentFullHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(this.itemResource, parent, false);

        return new StudentFullHolder(this.context, view);
    }

    @Override
    public void onBindViewHolder(final StudentFullHolder holder, final int position) {

        final StudentFull student = this.students.get(position);

        holder.bindStudent(student);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final StudentListFragment classUtils = new StudentListFragment();
                final String studentID = student.studentID;

                classUtils.openSingleStudent(studentID, context);
            }
        });

        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return (null != students ? this.students.size() : 0);
    }

    /**
     * Here is the key method to apply the animation
     */
    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.fade_in_up);

            if(position > 20){
                //animation.setStartOffset(position * 25 - 1250);
            } else {
                //animation.setStartOffset(position * 25);
            }
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(final StudentFullHolder holder) {
        holder.clearAnimation();
    }

    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                students = (List<StudentFull>) results.values; // has the filtered values
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                ArrayList<StudentFull> FilteredArrList = new ArrayList<StudentFull>();

                if (mOriginalValues == null) {
                    mOriginalValues = new ArrayList<StudentFull>(students); // saves the original data in mOriginalValues
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
                        String studentID = mOriginalValues.get(i).studentID;
                        String studentName = mOriginalValues.get(i).studentName;
                        String studentPhoto = mOriginalValues.get(i).studentPhoto;
                        if (studentName.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            StudentFull filteredStudent = new StudentFull(studentID, studentName, studentPhoto, null);
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

}
