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
import com.quickattend.quickattend.activity.UserListFragment;
import com.quickattend.quickattend.models.Student;
import com.quickattend.quickattend.models.StudentHolder;
import com.quickattend.quickattend.models.User;
import com.quickattend.quickattend.models.UserHolder;

import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<UserHolder> {

    private final List<User> users;
    private Context context;
    private int itemResource;

    // Allows to remember the last item shown on screen
    private int lastPosition = -1;

    public UserListAdapter(Context context, int itemResource, List<User> users) {

        this.users = users;
        this.context = context;
        this.itemResource = itemResource;
    }

    @Override
    public UserHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(this.itemResource, parent, false);

        return new UserHolder(this.context, view);
    }

    @Override
    public void onBindViewHolder(final UserHolder holder, final int position) {

        final User user = this.users.get(position);

        holder.bindUser(user);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final UserListFragment userUtils = new UserListFragment();
                final String userID = user.userID;

                userUtils.openSingleUser(userID, context);
            }
        });

        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return (null != users ? this.users.size() : 0);
    }

    /**
     * Here is the key method to apply the animation
     */
    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.fade_in_up);
            //animation.setStartOffset(position * 25);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(final UserHolder holder) {
        holder.clearAnimation();
    }

}
