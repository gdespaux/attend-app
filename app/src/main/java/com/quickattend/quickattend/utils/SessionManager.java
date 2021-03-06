package com.quickattend.quickattend.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class SessionManager {
    // LogCat tag
    private static String TAG = SessionManager.class.getSimpleName();

    // Shared Preferences
    SharedPreferences pref;

    Editor editor;
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "QuickAttendLogin";

    private static final String KEY_IS_LOGGEDIN = "isLoggedIn";
    private static final String KEY_REMEMBER_EMAIL = "rememberEmail";
    private static final String KEY_USER_EMAIL = "userEmail";

    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setLogin(boolean isLoggedIn) {

        editor.putBoolean(KEY_IS_LOGGEDIN, isLoggedIn);

        // commit changes
        editor.commit();

        Log.d(TAG, "User login session modified!");
    }

    public boolean isLoggedIn(){
        return pref.getBoolean(KEY_IS_LOGGEDIN, false);
    }

    public void setRememberEmail(boolean rememberEmail){
        editor.putBoolean(KEY_REMEMBER_EMAIL, rememberEmail);

        editor.commit();
    }

    public boolean rememberEmail(){
        return pref.getBoolean(KEY_REMEMBER_EMAIL, false);
    }

    public void setUserEmail(String userEmail){
        editor.putString(KEY_USER_EMAIL, userEmail);

        editor.commit();
    }

    public String getUserEmail(){
        return pref.getString(KEY_USER_EMAIL, "");
    }
}
