package com.quickattend.quickattend.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.activity.ClassStudentListActivity;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.CircularNetworkImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.instabug.library.Instabug.getApplicationContext;

public class StudentListAdapter extends RecyclerView.Adapter<StudentHolder> {

    private final List<Student> students;
    private Context context;
    private int itemResource;

    // Allows to remember the last item shown on screen
    private int lastPosition = -1;

    public StudentListAdapter(Context context, int itemResource, List<Student> students) {

        this.students = students;
        this.context = context;
        this.itemResource = itemResource;
    }

    @Override
    public StudentHolder onCreateViewHolder(ViewGroup parent, int viewType){

        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(this.itemResource, parent, false);

        return new StudentHolder(this.context, view);
    }

    @Override
    public void onBindViewHolder(final StudentHolder holder, final int position){

        final Student student = this.students.get(position);

        holder.bindStudent(student);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ClassStudentListActivity classUtils = new ClassStudentListActivity();
                final String studentID = student.studentID;
                final String classID = student.classID;
                final String accountID = student.accountID;
                final String currentDate = student.currentDate;

                CheckBox studentPresent = (CheckBox) holder.itemView.findViewById(R.id.studentPresent);
                Boolean presentNow = !studentPresent.isChecked();
                studentPresent.setChecked(presentNow);

                classUtils.addStudentAttendance(studentID, presentNow, classID, accountID, currentDate, context);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final ClassStudentListActivity classUtils = new ClassStudentListActivity();

                final String studentID = student.studentID;
                final String classID = student.classID;

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete Student")
                        .setMessage("This will remove the student from the class. This will not delete the actual student.\nAre you sure?")
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                classUtils.deleteStudentFromClass(studentID, classID, context);
                                students.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, getItemCount());
                                dialog.dismiss();
                            }
                        });
                builder.show();

                return true;
            }
        });

        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount(){
        return (null != students ? this.students.size() : 0);
    }

    /**
     * Here is the key method to apply the animation
     */
    private void setAnimation(View viewToAnimate, int position){
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition)
        {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.fade_in_up);
            animation.setStartOffset(position * 100);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(final StudentHolder holder){
        holder.clearAnimation();
    }

}
