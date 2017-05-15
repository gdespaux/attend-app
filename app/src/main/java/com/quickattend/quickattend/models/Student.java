package com.quickattend.quickattend.models;

public class Student {

    public String studentID;
    public String studentName;
    public String studentPhoto;
    public String studentPresent;
    public String classID;
    public String accountID;
    public String currentDate;

    public Student(String studentID,
                   String studentName,
                   String studentPhoto,
                   String studentPresent,
                   String classID,
                   String accountID,
                   String currentDate){

        this.studentID = studentID;
        this.studentName = studentName;
        this.studentPhoto = studentPhoto;
        this.studentPresent = studentPresent;
        this.classID = classID;
        this.accountID = accountID;
        this.currentDate = currentDate;
    }
}
