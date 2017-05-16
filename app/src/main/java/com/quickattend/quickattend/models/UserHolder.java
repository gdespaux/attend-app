package com.quickattend.quickattend.models;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.CircularNetworkImageView;

public class UserHolder extends RecyclerView.ViewHolder {
    private final TextView userID;
    private final TextView userName;
    private final CircularNetworkImageView userPhoto;

    ImageLoader imageLoader = AppController.getInstance().getImageLoader();

    private User user;
    private Context context;

    public UserHolder(Context context, View itemView){
        super(itemView);

        this.context = context;

        this.userID = (TextView) itemView.findViewById(R.id.userID);
        this.userName = (TextView) itemView.findViewById(R.id.userName);
        this.userPhoto = (CircularNetworkImageView) itemView.findViewById(R.id.userPhoto);
    }

    public void bindUser(User user){

        this.user = user;
        this.userID.setText(user.userID);
        this.userName.setText(user.userName);
        this.userPhoto.setImageUrl(user.userPhoto, imageLoader);
    }

    public void clearAnimation(){
        this.itemView.clearAnimation();
    }
}
