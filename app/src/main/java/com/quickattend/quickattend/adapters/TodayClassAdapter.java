package com.quickattend.quickattend.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;

import com.quickattend.quickattend.R;
import com.quickattend.quickattend.activity.ClassStudentListActivity;
import com.quickattend.quickattend.activity.HomeFragment;
import com.quickattend.quickattend.models.ClassHolder;
import com.quickattend.quickattend.models.ClassModel;

import java.util.List;

public class TodayClassAdapter extends RecyclerView.Adapter<ClassHolder> {

    private final List<ClassModel> classes;
    private Context context;
    private int itemResource;

    // Allows to remember the last item shown on screen
    private int lastPosition = -1;

    public TodayClassAdapter(Context context, int itemResource, List<ClassModel> classes) {

        this.classes = classes;
        this.context = context;
        this.itemResource = itemResource;
    }

    @Override
    public ClassHolder onCreateViewHolder(ViewGroup parent, int viewType){

        View view = LayoutInflater.from(parent.getContext())
                .inflate(this.itemResource, parent, false);

        return new ClassHolder(this.context, view);
    }

    @Override
    public void onBindViewHolder(final ClassHolder holder, final int position){

        final ClassModel theClass = this.classes.get(position);

        holder.bindClass(theClass);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final HomeFragment classUtils = new HomeFragment();
                final String classID = theClass.classID;
                final String className = theClass.className;

                classUtils.openSingleClass(classID, className, context);
            }
        });

        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount(){
        return (null != classes ? this.classes.size() : 0);
    }

    /**
     * Here is the key method to apply the animation
     */
    private void setAnimation(View viewToAnimate, int position){
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition)
        {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.fade_in_up);
            //animation.setStartOffset(position * 100);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(final ClassHolder holder){
        holder.clearAnimation();
    }
}
