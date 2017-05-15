package com.quickattend.quickattend.models;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.CircularNetworkImageView;

public class StudentFullHolder extends RecyclerView.ViewHolder {
    private final TextView studentID;
    private final TextView studentName;
    private final CircularNetworkImageView studentPhoto;

    ImageLoader imageLoader = AppController.getInstance().getImageLoader();

    private StudentFull student;
    private Context context;

    public StudentFullHolder(Context context, View itemView){
        super(itemView);

        this.context = context;

        this.studentID = (TextView) itemView.findViewById(R.id.studentID);
        this.studentName = (TextView) itemView.findViewById(R.id.studentName);
        this.studentPhoto = (CircularNetworkImageView) itemView.findViewById(R.id.studentPhoto);
    }

    public void bindStudent(StudentFull student){

        this.student = student;
        this.studentID.setText(student.studentID);
        this.studentName.setText(student.studentName);
        this.studentPhoto.setImageUrl(student.studentPhoto, imageLoader);

    }

    public void clearAnimation(){
        this.itemView.clearAnimation();
    }
}