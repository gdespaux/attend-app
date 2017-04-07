package com.classieapp.attend.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.ChangeBounds;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.classieapp.attend.R;
import com.classieapp.attend.app.AppConfig;
import com.classieapp.attend.app.AppController;
import com.classieapp.attend.utils.GeofenceTransitionService;
import com.classieapp.attend.utils.NotificationUtils;
import com.classieapp.attend.utils.SQLiteHandler;
import com.classieapp.attend.utils.SessionManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SingleStudentActivity extends AppCompatActivity {

    private static final String TAG = SingleStudentActivity.class.getSimpleName();

    private TextView mLatitudeText;
    private TextView mLongitudeText;
    private Button startClassButton;
    private Button showStudentsButton;

    private TextView classNameText;
    private TextView classTimeText;
    private TextView classLocationText;
    private TextView classDaysText;

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String userID;

    private String classID;
    private String className;
    private String classLocation;
    private String classTime;
    private static final int STATIC_RESULT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Transition fade = new Fade();
        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);

        Transition explode = new Explode();
        explode.excludeTarget(android.R.id.statusBarBackground, true);
        explode.excludeTarget(android.R.id.navigationBarBackground, true);

        Transition slide = new Slide();
        slide.excludeTarget(android.R.id.statusBarBackground, true);
        slide.excludeTarget(android.R.id.navigationBarBackground, true);

        // inside your activity (if you did not enable transitions in your theme)
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        // set an exit transition
        getWindow().setEnterTransition(slide);
        getWindow().setExitTransition(explode);
        getWindow().setAllowEnterTransitionOverlap(true);

        setContentView(R.layout.activity_single_class);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mLatitudeText = (TextView) findViewById(R.id.latLoc);
        mLongitudeText = (TextView) findViewById(R.id.lonLoc);
        startClassButton = (Button) findViewById(R.id.btnStartClass);
        showStudentsButton = (Button) findViewById(R.id.btnShowStudents);
        classNameText = (TextView) findViewById(R.id.className);
        classTimeText = (TextView) findViewById(R.id.classTime);
        classLocationText = (TextView) findViewById(R.id.classLocation);
        classDaysText = (TextView) findViewById(R.id.classDays);

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        // Fetching user details from sqlite
        final HashMap<String, String> user = db.getUserDetails();

        userID = user.get("uid");
        String name = user.get("name");
        String email = user.get("email");

        classID = getIntent().getStringExtra("classID");
        getSingleClass(classID);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_single_class, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_update_class:

                // Launching the add class activity
                Intent intent = new Intent(SingleStudentActivity.this, AddStudentActivity.class);
                intent.putExtra("editMode", true);
                intent.putExtra("classID", classID);
                intent.putExtra("className", classNameText.getText());
                startActivityForResult(intent, STATIC_RESULT, ActivityOptions.makeSceneTransitionAnimation(SingleStudentActivity.this).toBundle());

                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void showStudent() {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(JSON_STRING);

            JSONObject jo = jsonObject.getJSONObject("class");
            JSONObject cdObject = jo.getJSONObject("classDays");

            String className = jo.getString("className");
            String classLocation = jo.getString("classLocation");
            String classTime = jo.getString("classTime");
            String classLat = jo.getString("classLat");
            String classLng = jo.getString("classLng");

            getSupportActionBar().setTitle(className + " " + classTime);

            this.className = className;
            this.classLocation = classLocation;
            this.classTime = classTime;


            classNameText.setText(className);
            classLocationText.setText(classLocation);
            classTimeText.setText(classTime);

            //fenceLat = Double.parseDouble(classLat);
            //fenceLong = Double.parseDouble(classLng);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * Function to get selected student from MySQL DB
     */
    private void getSingleClass(final String classID) {
        // Tag used to cancel the request
        String tag_string_req = "req_get_single_student";


        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_GET_SINGLE_STUDENT, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Get Student Response: " + response.toString());

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        JSON_STRING = response;
                        showStudent();
                        //Toast.makeText(getApplicationContext(), "Classes loaded!", Toast.LENGTH_LONG).show();
                    } else {

                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Fetching Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("classID", classID);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult()", Integer.toString(resultCode));

        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {

            case STATIC_RESULT:
                if(resultCode == Activity.RESULT_OK){
                    getSingleClass(classID);
                }
                break;

        }
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(SingleStudentActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
