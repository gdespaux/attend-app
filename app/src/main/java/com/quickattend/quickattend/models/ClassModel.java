package com.quickattend.quickattend.models;

public class ClassModel {

    public String classID;
    public String className;
    public String classTime;
    public String classLocation;
    public String classCount;

    public ClassModel(String classID,
                      String className,
                      String classTime,
                      String classLocation,
                      String classCount){

        this.classID = classID;
        this.className = className;
        this.classTime = classTime;
        this.classLocation = classLocation;
        this.classCount = classCount;
    }
}
