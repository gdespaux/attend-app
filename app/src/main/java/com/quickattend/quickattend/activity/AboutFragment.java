package com.quickattend.quickattend.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.content.Intent;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.quickattend.quickattend.BuildConfig;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;

public class AboutFragment extends Fragment {
    private static final String TAG = AboutFragment.class.getSimpleName();

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String userID;
    private String accountID;

    private TextView buildNumberText;
    private TextView buildTypeText;
    private TextView appVersionText;
    private TextView androidVersionText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("About");

        // SqLite database handler
        db = new SQLiteHandler(getActivity());

        // session manager
        session = new SessionManager(getActivity());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        // Fetching user details from sqlite
        final HashMap<String, String> user = db.getUserDetails();

        userID = user.get("uid");
        accountID = user.get("account_id");
        String name = user.get("name");
        String email = user.get("email");

        View view = inflater.inflate(R.layout.fragment_about, container, false);

        buildNumberText = (TextView) view.findViewById(R.id.buildNumber);
        buildTypeText = (TextView) view.findViewById(R.id.buildType);
        appVersionText = (TextView) view.findViewById(R.id.appVersion);
        androidVersionText = (TextView) view.findViewById(R.id.androidVersion);

        getAppInfo();

        return view;

    }

    @Override
    public void onResume() {
        super.onResume();
        getAppInfo();
    }

    private void getAppInfo(){

        buildNumberText.setText("Build Number: " + Integer.toString(BuildConfig.VERSION_CODE));
        buildTypeText.setText("Build Type: " + BuildConfig.BUILD_TYPE);
        appVersionText.setText("App Version: " + BuildConfig.VERSION_NAME);
        androidVersionText.setText("Android OS: " + Build.VERSION.RELEASE);

    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     * */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
    }

}
