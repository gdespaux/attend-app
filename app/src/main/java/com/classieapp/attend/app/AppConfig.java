package com.classieapp.attend.app;

public class AppConfig {
    // Server user login url
    public static String URL_LOGIN = "http://classie.nhctechservices.com/login.php";

    // Server user register url
    public static String URL_REGISTER = "http://classie.nhctechservices.com/register.php";

    // Server add class url
    public static String URL_ADD_CLASS = "http://classie.nhctechservices.com/addClass.php";

    // Server get all classes url
    public static String URL_GET_ALL_CLASSES = "http://classie.nhctechservices.com/getAllClasses.php";

    // Server get single class url
    public static String URL_GET_SINGLE_CLASS = "http://classie.nhctechservices.com/getClass.php";

    // Server add reg id url
    public static String URL_REG_ID = "http://classie.nhctechservices.com/addRegID.php";

    // Server start class url
    public static String URL_START_CLASS = "http://classie.nhctechservices.com/startThisClass.php";

    // global topic to receive app wide push notifications
    public static final String TOPIC_GLOBAL = "global";

    // broadcast receiver intent filters
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final String PUSH_NOTIFICATION = "pushNotification";

    // id to handle the notification in the notification tray
    public static final int NOTIFICATION_ID = 100;
    public static final int NOTIFICATION_ID_BIG_IMAGE = 101;

    public static final String SHARED_PREF = "qa_firebase";
}