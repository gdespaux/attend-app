package com.quickattend.quickattend.models;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.quickattend.quickattend.R;

public class ClassHolder extends RecyclerView.ViewHolder {

    private final TextView classID;
    private final TextView className;
    private final TextView classTime;
    private final TextView classLocation;
    private final TextView classCount;

    private ClassModel theClassModel;
    private Context context;

    public ClassHolder(Context context, View itemView){
        super(itemView);

        this.context = context;

        this.classID = (TextView) itemView.findViewById(R.id.classID);
        this.className = (TextView) itemView.findViewById(R.id.className);
        this.classTime = (TextView) itemView.findViewById(R.id.classTime);
        this.classLocation = (TextView) itemView.findViewById(R.id.classLocation);
        this.classCount = (TextView) itemView.findViewById(R.id.classCount);
    }

    public void bindClass(ClassModel theClassModel){

        this.theClassModel = theClassModel;
        this.classID.setText(theClassModel.classID);
        this.className.setText(theClassModel.className);
        this.classTime.setText(theClassModel.classTime);
        this.classLocation.setText(theClassModel.classLocation);
        this.classCount.setText(theClassModel.classCount);
    }

    public void clearAnimation(){
        this.itemView.clearAnimation();
    }
}
