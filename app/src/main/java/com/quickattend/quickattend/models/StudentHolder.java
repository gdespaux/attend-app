package com.quickattend.quickattend.models;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.models.Student;
import com.quickattend.quickattend.utils.CircularNetworkImageView;

public class StudentHolder extends RecyclerView.ViewHolder {
    private final TextView studentID;
    private final TextView classID;
    private final TextView studentName;
    private final CircularNetworkImageView studentPhoto;
    private final CheckBox studentPresent;

    ImageLoader imageLoader = AppController.getInstance().getImageLoader();

    private Student student;
    private Context context;

    public StudentHolder(Context context, View itemView){
        super(itemView);

        this.context = context;

        this.studentID = (TextView) itemView.findViewById(R.id.studentID);
        this.classID = (TextView) itemView.findViewById(R.id.classID);
        this.studentName = (TextView) itemView.findViewById(R.id.studentName);
        this.studentPhoto = (CircularNetworkImageView) itemView.findViewById(R.id.studentPhoto);
        this.studentPresent = (CheckBox) itemView.findViewById(R.id.studentPresent);
    }

    public void bindStudent(Student student){

        this.student = student;
        this.studentID.setText(student.studentID);
        this.classID.setText(student.classID);
        this.studentName.setText(student.studentName);
        this.studentPhoto.setImageUrl(student.studentPhoto, imageLoader);

        if (student.studentPresent.equals("yes")) {
            this.studentPresent.setChecked(true);
        } else {
            this.studentPresent.setChecked(false);
        }
    }

    public void clearAnimation(){
        this.itemView.clearAnimation();
    }
}
