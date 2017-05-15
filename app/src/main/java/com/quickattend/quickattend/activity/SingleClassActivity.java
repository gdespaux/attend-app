package com.quickattend.quickattend.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.app.ProgressDialog;
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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.GeofenceTransitionService;
import com.quickattend.quickattend.utils.NotificationUtils;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;
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
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import rx.Single;

public class SingleClassActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback {

    private static final String TAG = SingleClassActivity.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private ProgressDialog pDialog;

    private TextView mLatitudeText;
    private TextView mLongitudeText;
    private Button startClassButton;
    private Button showStudentsButton;

    private TextView classNameText;
    private TextView classTimeText;
    private TextView classLocationText;
    private TextView classDaysText;

    private final int REQUEST_PERMISSION_LOCATION = 1;
    private final int REQUEST_CHECK_SETTINGS = 2;

    private LocationRequest locationRequest;

    // Defined in milli seconds.
    // This number in extremely low, and should be used only for debug
    private final int UPDATE_INTERVAL = 5000;
    private final int FASTEST_INTERVAL = 3000;

    private static final long GEO_DURATION = 15000;
    private String geoFence_class_id = "";
    private static final float GEOFENCE_RADIUS = 15000.0f; // in meters

    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;

    private double fenceLat;
    private double fenceLong;

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String userID;

    private String classID;
    private String name;
    private String accountID;
    private String email;
    private String className;
    private String classLocation;
    private String classTime;
    private String[] onDays = new String[7];
    private static final int STATIC_RESULT = 3;

    private BroadcastReceiver mRegistrationBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_class);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Progress dialog
        pDialog = new ProgressDialog(SingleClassActivity.this);
        pDialog.setCancelable(false);

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
        accountID = user.get("account_id");
        name = user.get("name");
        email = user.get("email");

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        startClassButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startThisClass();
            }
        });

        showStudentsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Launching the add class activity
                Intent intent = new Intent(SingleClassActivity.this, ClassStudentListActivity.class);
                intent.putExtra("classID", classID);
                intent.putExtra("className", className);
                startActivity(intent);

            }
        });

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction().equals(AppConfig.PUSH_NOTIFICATION)) {
                    // new push notification is received

                    String message = intent.getStringExtra("message");
                    try {
                        JSONObject jsonObj = new JSONObject(intent.getStringExtra("payload"));
                        fenceLat = Double.parseDouble(jsonObj.getString("classLat"));
                        fenceLong = Double.parseDouble(jsonObj.getString("classLng"));

                        Log.d("FENCELAT", Double.toString(fenceLat));
                        Log.d("FENCELNG", Double.toString(fenceLong));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(getApplicationContext(), "Push notification: " + message, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Got ya message!");
                    startGeofence();
                }

                Log.d(TAG, "Uh oh!");
            }
        };

        classID = getIntent().getStringExtra("classID");
        getSingleClass(classID);

        new Instabug.Builder(getApplication(), AppConfig.INSTABUG_KEY)
                .setInvocationEvent(InstabugInvocationEvent.SHAKE)
                .build();
        Instabug.identifyUser(name, email);

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
                Intent intent = new Intent(SingleClassActivity.this, AddClassActivity.class);
                intent.putExtra("editMode", true);
                intent.putExtra("classID", classID);
                intent.putExtra("className", classNameText.getText());
                intent.putExtra("classTime", classTimeText.getText());
                intent.putExtra("classLocation", classLocationText.getText());
                intent.putExtra("onDays", onDays);
                startActivityForResult(intent, STATIC_RESULT);

                return true;

            case R.id.action_delete_class:
                AlertDialog.Builder builder = new AlertDialog.Builder(SingleClassActivity.this);
                builder.setTitle("Delete ClassModel")
                        .setMessage("This will delete the class and its attendance records! Are you sure?")
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteClass();
                                dialog.dismiss();
                            }
                        });
                builder.show();

                return true;

            case R.id.action_view_students:
                Intent i = new Intent(SingleClassActivity.this, ClassStudentListActivity.class);
                i.putExtra("classID", classID);
                i.putExtra("className", classNameText.getText());
                startActivity(i);

                return true;

            case R.id.action_export_attendance_by_date:
                exportAttendance();

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register GCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(AppConfig.REGISTRATION_COMPLETE));

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(AppConfig.PUSH_NOTIFICATION));

        // clear the notification area when the app is opened
        NotificationUtils.clearNotifications(getApplicationContext());
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    /**
     * Export attendance from DB to user email
     * */
    private void exportAttendance() {
        // Tag used to cancel the request
        String tag_string_req = "req_export_attendance_by_date";

        pDialog.setMessage("Exporting Attendance...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_EXPORT_ATTENDANCE_BY_DATE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Export Attendance Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        hideDialog();
                        Toast.makeText(SingleClassActivity.this, "Attendance exported. Please check your email", Toast.LENGTH_LONG).show();
                    } else {

                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(SingleClassActivity.this,
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
                Toast.makeText(SingleClassActivity.this,
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountID", accountID);
                params.put("userEmail", email);
                params.put("userName", name);
                params.put("classID", classID);
                params.put("className", className);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Function to start checking for students in class will post params(class id)
     * to class url
     */
    public void startThisClass() {

        if (classID != null) {
            // Tag used to cancel the request
            String tag_string_req = "req_start_class";

            StringRequest strReq = new StringRequest(Request.Method.POST,
                    AppConfig.URL_START_CLASS, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Start ClassModel Response: " + response);
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Start ClassModel Error: " + error.getMessage());
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    // Posting params to class url
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("classID", classID);
                    params.put("classLat", String.valueOf(mLastLocation.getLatitude()));
                    params.put("classLng", String.valueOf(mLastLocation.getLongitude()));

                    Log.e(TAG, "classID: " + classID);

                    return params;
                }

            };

            // Adding request to request queue
            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
        }
    }

    /**
     * Function to start checking for students in class will post params(class id)
     * to class url
     */
    public void attendThisClass() {

        if (classID != null) {
            // Tag used to cancel the request
            String tag_string_req = "req_start_class";

            StringRequest strReq = new StringRequest(Request.Method.POST,
                    AppConfig.URL_START_CLASS, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Start ClassModel Response: " + response);
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Start ClassModel Error: " + error.getMessage());
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    // Posting params to class url
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("classID", classID);
                    params.put("classLat", String.valueOf(mLastLocation.getLatitude()));
                    params.put("classLng", String.valueOf(mLastLocation.getLongitude()));

                    Log.e(TAG, "classID: " + classID);

                    return params;
                }

            };

            // Adding request to request queue
            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
        }
    }

    private void showClass() {
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

            onDays[0] = cdObject.getString("Sunday");
            onDays[1] = cdObject.getString("Monday");
            onDays[2] = cdObject.getString("Tuesday");
            onDays[3] = cdObject.getString("Wednesday");
            onDays[4] = cdObject.getString("Thursday");
            onDays[5] = cdObject.getString("Friday");
            onDays[6] = cdObject.getString("Saturday");

            getSupportActionBar().setTitle(className + " " + classTime);

            this.className = className;
            this.classLocation = classLocation;
            this.classTime = classTime;


            classNameText.setText(className);
            classLocationText.setText(classLocation);
            classTimeText.setText(classTime);

            classDaysText.setText("");
            if (onDays[0].equals("true")) classDaysText.append("Sunday, ");
            if (onDays[1].equals("true")) classDaysText.append("Monday, ");
            if (onDays[2].equals("true")) classDaysText.append("Tuesday, ");
            if (onDays[3].equals("true")) classDaysText.append("Wednesday, ");
            if (onDays[4].equals("true")) classDaysText.append("Thursday, ");
            if (onDays[5].equals("true")) classDaysText.append("Friday, ");
            if (onDays[6].equals("true")) classDaysText.append("Saturday");

            geoFence_class_id = className;
            //fenceLat = Double.parseDouble(classLat);
            //fenceLong = Double.parseDouble(classLng);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * Function to set selected class deleted from MySQL DB
     */
    private void deleteClass(){
        // Tag used to cancel the request
        String tag_string_req = "req_delete_class";


        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_DELETE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Delete ClassModel Response: " + response);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        JSON_STRING = response;
                        finish();
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
                params.put("deleteType", "classes");
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Function to get selected class from MySQL DB
     */
    private void getSingleClass(final String classID) {
        // Tag used to cancel the request
        String tag_string_req = "req_get_single_class";


        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_GET_SINGLE_CLASS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Get ClassModel Response: " + response);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        JSON_STRING = response;
                        showClass();
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

    public static Intent makeNotificationIntent(Context geofenceService, String msg) {
        Log.d(TAG, msg);
        return new Intent(geofenceService, SingleClassActivity.class);
    }

    // Start Geofence creation process (Overall Geofence function)
    private void startGeofence() {
        Log.i(TAG, "startGeofence()");
        if (fenceLat != 0 && fenceLong != 0) {
            Geofence geofence = createGeofence(fenceLat, fenceLong, GEOFENCE_RADIUS);
            GeofencingRequest geofenceRequest = createGeofenceRequest(geofence);
            addGeofence(geofenceRequest);
        } else {
            Log.e(TAG, "Geofence marker is null");
        }
    }

    // Create a Geofence
    private Geofence createGeofence(double latitude, double longitude, float radius) {
        Log.d(TAG, "createGeofence");
        return new Geofence.Builder()
                .setRequestId(geoFence_class_id)
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent");
        if (geoFencePendingIntent != null)
            return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceTransitionService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Log.d(TAG, "addGeofence");
        if (checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.i(TAG, "onResult: " + result);
        if (result.getStatus().isSuccess()) {
            //drawGeofence();
        } else {
            // inform about fail
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                showExplanation("Location Permission Required", "We need access to your location to show you nearby classes!", Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_PERMISSION_LOCATION);
            } else {
                requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_PERMISSION_LOCATION);
            }

            Log.e("LOC ERROR", "no perms!");
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
            Log.i("Location Info: ", "Location found!");
        } else {
            Log.e("LOC ERROR", "mLL is null!");
        }
        startLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(findViewById(R.id.singleClassLayout), "Permission Granted!", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.ACCESS_FINE_LOCATION)) {
                            showExplanation("Location Permission Required", "We need access to your location to show you nearby classes!", Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_PERMISSION_LOCATION);
                        } else {
                            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_PERMISSION_LOCATION);
                        }

                        Log.e("LOC ERROR", "no perms!");
                        return;
                    }
                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                            mGoogleApiClient);
                    if (mLastLocation != null) {
                        mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
                        mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
                        Log.i("Location Info: ", "Location found!");
                    } else {
                        Log.e("LOC ERROR", "mLL is null!");
                    }
                    startLocationUpdates();
                } else {
                    Snackbar.make(findViewById(R.id.singleClassLayout), "Permission Denied!", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
        }
    }

    // Start location Updates
    private void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if (checkPermission()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

            //check to see if high accuracy location is enabled
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    final LocationSettingsStates states = result.getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can
                            // initialize location requests here.
                            //Snackbar.make(findViewById(R.id.singleClassLayout), "Settings are correct!", Snackbar.LENGTH_SHORT)
                            //        .setAction("Action", null).show();
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied, but this can be fixed
                            // by showing the user a dialog.
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(
                                        SingleClassActivity.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way
                            // to fix the settings so we won't show the dialog.
                            Snackbar.make(findViewById(R.id.singleClassLayout), "Settings can't be fixed!", Snackbar.LENGTH_SHORT)
                                    .setAction("Action", null).show();
                            break;
                    }
                }
            });

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult()", Integer.toString(resultCode));

        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        // All required changes were successfully made
                        Snackbar.make(findViewById(R.id.singleClassLayout), "High Accuracy Location Enabled!", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        // The user was asked to change settings, but chose not to
                        Snackbar.make(findViewById(R.id.singleClassLayout), "User cancelled request!", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                        break;
                    }
                    default: {
                        break;
                    }
                }
                break;

            case STATIC_RESULT:
                if(resultCode == Activity.RESULT_OK){
                    getSingleClass(classID);
                }
                break;

        }
    }

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged [" + location + "]");
        //lastLocation = location;
        //writeActualLocation(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently

        Snackbar.make(findViewById(R.id.singleClassLayout), "Google Play Error!", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Snackbar.make(findViewById(R.id.singleClassLayout), "Google Play Suspended!", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    @Override
    protected void onStart() {
        //mGoogleApiClient.connect();
        Log.i("Connect Info: ", "Connection made!");
        super.onStart();
    }

    @Override
    protected void onStop() {

        /*
        try {
            ArrayList<String> geofencIds = new ArrayList<String>();
            geofencIds.add(geoFence_class_id);

            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofencIds)
                    .setResultCallback(new ResultCallback<Status>() {

                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {

                            }
                            // Remove notifiation here
                        }
                    });
        } catch (SecurityException securityException) {
            Snackbar.make(findViewById(R.id.singleClassLayout), securityException.getMessage(), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }

        mGoogleApiClient.disconnect();
        */
        Log.i("Connect Info: ", "Connection closed!");
        super.onStop();
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(SingleClassActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
