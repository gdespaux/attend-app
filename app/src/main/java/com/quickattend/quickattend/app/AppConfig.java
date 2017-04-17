package com.quickattend.quickattend.app;

import com.quickattend.quickattend.BuildConfig;

public class AppConfig {
    
    //Server Base URL
    public final static String BASE_URL = BuildConfig.BASE_URL;

    // Server user login url
    public static String URL_LOGIN = BASE_URL + "login.php";

    // Server user register url
    public static String URL_REGISTER = BASE_URL + "register.php";

    // Server add class url
    public static String URL_ADD_CLASS = BASE_URL + "addClass.php";

    // Server update class url
    public static String URL_UPDATE_CLASS = BASE_URL + "updateClass.php";

    // Server get all classes url
    public static String URL_GET_ALL_CLASSES = BASE_URL + "getAllClasses.php";

    // Server get todays classes url
    public static String URL_GET_TODAY_CLASSES = BASE_URL + "getTodayClasses.php";

    // Server get single class url
    public static String URL_GET_SINGLE_CLASS = BASE_URL + "getClass.php";

    // Server add reg id url
    public static String URL_REG_ID = BASE_URL + "addRegID.php";

    // Server start class url
    public static String URL_START_CLASS = BASE_URL + "startThisClass.php";

    // Server get class students url
    public static String URL_GET_CLASS_STUDENTS = BASE_URL + "getClassStudents.php";

    // Server get all students url
    public static String URL_GET_ALL_STUDENTS = BASE_URL + "getAllStudents.php";

    // Server get account students url
    public static String URL_TYPEAHEAD_ACCOUNT_STUDENTS = BASE_URL + "typeaheadAccountStudents.php";

    // Server add attendance url
    public static String URL_ADD_ATTENDANCE = BASE_URL + "addAttendance.php";

    // Server add student url
    public static String URL_ADD_STUDENT = BASE_URL + "addStudent.php";

    // Server update student url
    public static String URL_UPDATE_STUDENT = BASE_URL + "updateStudent.php";

    // Server get single student url
    public static String URL_GET_SINGLE_STUDENT = BASE_URL + "getStudent.php";

    // Server get chart attendance data url
    public static String URL_GET_CHART_ATTENDANCE = BASE_URL + "getChartAttendance.php";

    // Server set inactive student url
    public static String URL_SET_STUDENT_INACTIVE = BASE_URL + "setStudentInactive.php";

    // Server delete url
    public static String URL_DELETE = BASE_URL + "delete.php";

    // Server get user info url
    public static String URL_GET_USER_INFO = BASE_URL + "userInfo.php";

    // global topic to receive app wide push notifications
    public static final String TOPIC_GLOBAL = "global";

    // broadcast receiver intent filters
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final String PUSH_NOTIFICATION = "pushNotification";

    // id to handle the notification in the notification tray
    public static final int NOTIFICATION_ID = 100;
    public static final int NOTIFICATION_ID_BIG_IMAGE = 101;

    public static final String SHARED_PREF = "qa_firebase";

    //Change for production app
    public static final String INSTABUG_KEY = "5709e0e9e263367bce61600df8d3c749";
}