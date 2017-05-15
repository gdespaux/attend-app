package com.quickattend.quickattend.models;

public class StudentFull {

    public String studentID;
    public String studentName;
    public String studentPhoto;
    public String accountID;

    public StudentFull(String studentID,
                   String studentName,
                   String studentPhoto,
                   String accountID){

        this.studentID = studentID;
        this.studentName = studentName;
        this.studentPhoto = studentPhoto;
        this.accountID = accountID;
    }
}
