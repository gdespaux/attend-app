package com.quickattend.quickattend.models;

public class User {

    public String userID;
    public String userName;
    public String userPhoto;

    public User(String userID, String userName, String userPhoto){

        this.userID = userID;
        this.userName = userName;
        this.userPhoto = userPhoto;
    }
}