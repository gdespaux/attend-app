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
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.classieapp.attend.R;
import com.classieapp.attend.app.AppConfig;
import com.classieapp.attend.app.AppController;
import com.classieapp.attend.utils.CircularNetworkImageView;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SingleStudentActivity extends AppCompatActivity {

    private static final String TAG = SingleStudentActivity.class.getSimpleName();

    private TextView mLatitudeText;
    private TextView mLongitudeText;
    private Button startClassButton;
    private Button showStudentsButton;

    private TextView studentNameText;
    private TextView studentDOBText;
    private TextView studentAgeText;
    private TextView studentPhoneText;
    private TextView studentGenderText;
    private CircularNetworkImageView studentPhoto;

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String userID;

    private String studentID;
    private String studentName;
    private String studentDOB;
    private String studentPhone;
    private String studentGender;
    private static final int STATIC_RESULT = 4;

    ImageLoader imageLoader = AppController.getInstance().getImageLoader();
    private Calendar studentCalendar = Calendar.getInstance();

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

        setContentView(R.layout.activity_single_student);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        studentNameText = (TextView) findViewById(R.id.studentName);
        studentDOBText = (TextView) findViewById(R.id.studentDOB);
        studentAgeText = (TextView) findViewById(R.id.studentAge);
        studentPhoneText = (TextView) findViewById(R.id.studentPhone);
        studentGenderText = (TextView) findViewById(R.id.studentGender);
        studentPhoto = (CircularNetworkImageView) findViewById(R.id.studentPhoto);

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

        studentID = getIntent().getStringExtra("studentID");
        getSingleStudent();

    }

    /**
     * Method to extract the user's age from the entered Date of Birth.
     *
     * @return ageS String The user's age in years based on the supplied DoB.
     */
    private String getAge(int year, int month, int day){
        Calendar dob = Calendar.getInstance();
        Calendar today = Calendar.getInstance();

        dob.set(year, month, day);

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)){
            age--;
        }

        Integer ageInt = new Integer(age);
        String ageS = ageInt.toString();

        return ageS;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_single_student, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_update_student:

                // Launching the add class activity
                Intent intent = new Intent(SingleStudentActivity.this, AddStudentActivity.class);
                intent.putExtra("editMode", true);
                intent.putExtra("studentID", studentID);
                intent.putExtra("studentName", studentName);
                intent.putExtra("studentDOB", studentDOB);
                intent.putExtra("studentPhone", studentPhone);
                intent.putExtra("studentGender", studentGender);
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

            JSONObject jo = jsonObject.getJSONObject("student");
            //JSONObject cdObject = jo.getJSONObject("classDays");

            String studentName = jo.getString("studentName");
            String studentDOB = jo.getString("studentDOB");
            String studentPhone = jo.getString("studentPhone");
            String studentGender = jo.getString("studentGender");
            String studentPhoto = jo.getString("studentPhoto");

            getSupportActionBar().setTitle(studentName);

            this.studentName = studentName;
            this.studentDOB = studentDOB;
            Log.i("DOB", studentDOB);
            this.studentPhone = studentPhone;
            this.studentGender = studentGender;

            String myFormat = "yyyy-MM-dd"; //In which you need put here
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
            String newFormat = "MM/dd/yyyy"; //In which you need put here
            SimpleDateFormat newSdf = new SimpleDateFormat(newFormat, Locale.US);

            studentCalendar.setTime(sdf.parse(studentDOB));
            int year = studentCalendar.get(Calendar.YEAR);
            int month = studentCalendar.get(Calendar.MONTH);
            int day = studentCalendar.get(Calendar.DAY_OF_MONTH);

            if(!studentDOB.equals("0000-00-00")){
                studentDOBText.setText(newSdf.format(sdf.parse(studentDOB)));
                studentAgeText.setText("Age: " + getAge(year, month, day));
            } else {
                studentDOBText.setText("");
                studentAgeText.setText("");
            }

            studentNameText.setText(studentName);
            //studentDOBText.setText(studentDOB);
            studentPhoneText.setText(studentPhone);
            studentGenderText.setText(studentGender);
            this.studentPhoto.setImageUrl(studentPhoto, imageLoader);

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    /**
     * Function to get selected student from MySQL DB
     */
    private void getSingleStudent() {
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
                params.put("studentID", studentID);
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
                    getSingleStudent();
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
