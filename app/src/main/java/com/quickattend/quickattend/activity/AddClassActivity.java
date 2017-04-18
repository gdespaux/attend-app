package com.quickattend.quickattend.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.PlaceArrayAdapter;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddClassActivity extends AppCompatActivity implements OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = AddClassActivity.class.getSimpleName();
    private Button btnAddClass;
    private ProgressDialog pDialog;
    private EditText inputClassName;
    private EditText inputClassTime;
    private EditText inputClassLocation;
    private TextView inputClassLat;
    private TextView inputClassLng;

    private ToggleButton toggleSunday;
    private ToggleButton toggleMonday;
    private ToggleButton toggleTuesday;
    private ToggleButton toggleWednesday;
    private ToggleButton toggleThursday;
    private ToggleButton toggleFriday;
    private ToggleButton toggleSaturday;

    private boolean editMode = false;

    private SQLiteHandler db;
    private SessionManager session;

    private String userID;
    private String email;
    private String accountID;
    private String classTime;
    private String classID;
    private String[] onDays = new String[7];

    private boolean didSelectItem = false;

    private GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mPlaceArrayAdapter;
    private static final int GOOGLE_API_CLIENT_ID = 0;
    private AutoCompleteTextView mAutocompleteTextView;
    private static final LatLngBounds BOUNDS_NEW_ORLEANS = new LatLngBounds(
            new LatLng(29.867881, -89.597667), new LatLng(29.940349, -90.413854));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mGoogleApiClient = new GoogleApiClient.Builder(AddClassActivity.this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();
        mAutocompleteTextView = (AutoCompleteTextView) findViewById(R.id
                .classLocation);
        mAutocompleteTextView.setThreshold(3);
        mAutocompleteTextView.setOnItemClickListener(mAutocompleteClickListener);
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES)
                .setCountry("US")
                .build();
        mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1,
                BOUNDS_NEW_ORLEANS, typeFilter);
        mAutocompleteTextView.setAdapter(mPlaceArrayAdapter);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        btnAddClass = (Button) findViewById(R.id.btnAddClass);
        inputClassName = (EditText) findViewById(R.id.className);
        inputClassTime = (EditText) findViewById(R.id.classTime);
        inputClassLocation = (EditText) findViewById(R.id.classLocation);
        inputClassLat = (TextView) findViewById(R.id.classLat);
        inputClassLng = (TextView) findViewById(R.id.classLng);

        toggleSunday = (ToggleButton) findViewById(R.id.toggleSunday);
        toggleMonday = (ToggleButton) findViewById(R.id.toggleMonday);
        toggleTuesday = (ToggleButton) findViewById(R.id.toggleTuesday);
        toggleWednesday = (ToggleButton) findViewById(R.id.toggleWednesday);
        toggleThursday = (ToggleButton) findViewById(R.id.toggleThursday);
        toggleFriday = (ToggleButton) findViewById(R.id.toggleFriday);
        toggleSaturday = (ToggleButton) findViewById(R.id.toggleSaturday);

        if(getIntent().hasExtra("editMode")){
            editMode = true;
            didSelectItem = true;
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            inputClassName.setText(getIntent().getStringExtra("className"));
            inputClassTime.setText(getIntent().getStringExtra("classTime"));
            inputClassLocation.setText(getIntent().getStringExtra("classLocation"));

            classTime = getIntent().getStringExtra("classTime");
            classID = getIntent().getStringExtra("classID");

            toggleSunday.setChecked(getIntent().getStringArrayExtra("onDays")[0].equals("true"));
            toggleMonday.setChecked(getIntent().getStringArrayExtra("onDays")[1].equals("true"));
            toggleTuesday.setChecked(getIntent().getStringArrayExtra("onDays")[2].equals("true"));
            toggleWednesday.setChecked(getIntent().getStringArrayExtra("onDays")[3].equals("true"));
            toggleThursday.setChecked(getIntent().getStringArrayExtra("onDays")[4].equals("true"));
            toggleFriday.setChecked(getIntent().getStringArrayExtra("onDays")[5].equals("true"));
            toggleSaturday.setChecked(getIntent().getStringArrayExtra("onDays")[6].equals("true"));
        } else {
            //currentDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());
        }


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
        email = user.get("email");
        accountID = user.get("account_id");

        // View classes click event
        btnAddClass.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String className = inputClassName.getText().toString().trim();
                String classTime = inputClassTime.getText().toString();
                String classLocation = inputClassLocation.getText().toString().trim();
                String classLat = inputClassLat.getText().toString().trim();
                String classLng = inputClassLng.getText().toString().trim();

                String[] onDays = new String[7];

                onDays[0] = toggleSunday.isChecked() ? "true" : "false";
                onDays[1] = toggleMonday.isChecked() ? "true" : "false";
                onDays[2] = toggleTuesday.isChecked() ? "true" : "false";
                onDays[3] = toggleWednesday.isChecked() ? "true" : "false";
                onDays[4] = toggleThursday.isChecked() ? "true" : "false";
                onDays[5] = toggleFriday.isChecked() ? "true" : "false";
                onDays[6] = toggleSaturday.isChecked() ? "true" : "false";

                if(didSelectItem){
                    if (!className.isEmpty() && !classTime.isEmpty() && !classLocation.isEmpty() && !classLat.isEmpty() && !classLng.isEmpty()) {
                        addClass(userID, className, classTime, classLocation, onDays, classLat, classLng);
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Please enter class details!", Toast.LENGTH_LONG)
                                .show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Please select your location from the list!", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        inputClassTime.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    Calendar mcurrentTime = Calendar.getInstance();
                    int hour;
                    int minute;

                    if(editMode){
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
                        Date date = null;
                        try {
                            date = sdf.parse(classTime);
                        } catch (ParseException e) {
                        }
                        Calendar c = Calendar.getInstance();
                        c.setTime(date);

                        hour = c.get(Calendar.HOUR_OF_DAY);
                        minute = c.get(Calendar.MINUTE);
                    } else {
                        hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                        minute = mcurrentTime.get(Calendar.MINUTE);
                    }

                    TimePickerDialog mTimePicker;
                    mTimePicker = new TimePickerDialog(AddClassActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
                            int hour = hourOfDay % 12;
                            if(hour == 0){
                                hour = 12;
                            }
                            inputClassTime.setText(String.format(Locale.US, "%2d:%02d %s", hour, minute,
                                    hourOfDay < 12 ? "am" : "pm"));
                        }
                    }, hour, minute, false);//Yes 24 hour time
                    mTimePicker.setTitle("Select Time");
                    mTimePicker.show();
                }
            }
        });

        inputClassLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                didSelectItem = false;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        new Instabug.Builder(getApplication(), AppConfig.INSTABUG_KEY)
                .setInvocationEvent(InstabugInvocationEvent.SHAKE)
                .build();
        Instabug.identifyUser(email, email);
    }

    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            Log.i(TAG, "Selected: " + item.description);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            Log.i(TAG, "Fetching details for ID: " + item.placeId);
            didSelectItem = true; //true when item selected from list
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e(TAG, "Place query did not complete. Error: " +
                        places.getStatus().toString());
                return;
            }
            // Selecting the first object buffer.
            final Place place = places.get(0);
            final LatLng location = place.getLatLng();

            inputClassLat.setText(Double.toString(location.latitude));
            inputClassLng.setText(Double.toString(location.longitude));
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(editMode){
            getMenuInflater().inflate(R.menu.menu_update_class, menu);
        } else{
            getMenuInflater().inflate(R.menu.menu_add_class, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        String className = inputClassName.getText().toString().trim();
        String classTime = inputClassTime.getText().toString();
        String classLocation = inputClassLocation.getText().toString().trim();
        String classLat = inputClassLat.getText().toString().trim();
        String classLng = inputClassLng.getText().toString().trim();

        String[] onDays = new String[7];

        switch (item.getItemId()) {
            case R.id.action_add_class:

                onDays[0] = toggleSunday.isChecked() ? "true" : "false";
                onDays[1] = toggleMonday.isChecked() ? "true" : "false";
                onDays[2] = toggleTuesday.isChecked() ? "true" : "false";
                onDays[3] = toggleWednesday.isChecked() ? "true" : "false";
                onDays[4] = toggleThursday.isChecked() ? "true" : "false";
                onDays[5] = toggleFriday.isChecked() ? "true" : "false";
                onDays[6] = toggleSaturday.isChecked() ? "true" : "false";


                if(didSelectItem){
                    if (!className.isEmpty() && !classTime.isEmpty() && !classLocation.isEmpty() && !classLat.isEmpty() && !classLng.isEmpty()) {
                        addClass(userID, className, classTime, classLocation, onDays, classLat, classLng);

                        return true;
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Please enter class details!", Toast.LENGTH_LONG)
                                .show();
                        return false;
                    }
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Please select your location from the list!", Toast.LENGTH_LONG)
                            .show();
                    return false;
                }

            case R.id.action_update_class:

                onDays[0] = toggleSunday.isChecked() ? "true" : "false";
                onDays[1] = toggleMonday.isChecked() ? "true" : "false";
                onDays[2] = toggleTuesday.isChecked() ? "true" : "false";
                onDays[3] = toggleWednesday.isChecked() ? "true" : "false";
                onDays[4] = toggleThursday.isChecked() ? "true" : "false";
                onDays[5] = toggleFriday.isChecked() ? "true" : "false";
                onDays[6] = toggleSaturday.isChecked() ? "true" : "false";


                if(didSelectItem){
                    if (!className.isEmpty() && !classTime.isEmpty() && !classLocation.isEmpty()) {
                        updateClass(userID, className, classTime, classLocation, onDays, classLat, classLng);

                        return true;
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Please enter class details!", Toast.LENGTH_LONG)
                                .show();
                        return false;
                    }
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Please select your location from the list!", Toast.LENGTH_LONG)
                            .show();
                    return false;
                }

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     * */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(AddClassActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Function to store user in MySQL database will post params(tag, name,
     * email, password) to register url
     * */
    private void addClass(final String userID, final String className, final String classTime, final String classLocation, final String[] onDays, final String classLat, final String classLng) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_class";

        pDialog.setMessage("Adding Class...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_ADD_CLASS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Add Class Response: " + response.toString());
                hideDialog();

                inputClassName.setText("");
                inputClassTime.setText("");
                inputClassLocation.setText("");
                inputClassLat.setText("");
                inputClassLng.setText("");

                toggleSunday.setChecked(false);
                toggleMonday.setChecked(false);
                toggleTuesday.setChecked(false);
                toggleWednesday.setChecked(false);
                toggleThursday.setChecked(false);
                toggleFriday.setChecked(false);
                toggleSaturday.setChecked(false);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        // Class successfully stored in MySQL
                        // Now store the class in sqlite
                        String uid = jObj.getString("uid");

                        JSONObject classInfo = jObj.getJSONObject("class");
                        String className = classInfo.getString("className");
                        String classTime = classInfo.getString("classTime");
                        String classLocation = classInfo.getString("classLocation");
                        String created_at = classInfo
                                .getString("created_at");

                        // Inserting row in users table
                        //db.addUser(name, email, uid, created_at);

                        Toast.makeText(getApplicationContext(), "Class added!", Toast.LENGTH_LONG).show();
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
                Log.e(TAG, "Creation Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("userID", userID);
                params.put("accountID", accountID);
                params.put("className", className);
                params.put("classTime", classTime);
                params.put("classLocation", classLocation);
                params.put("classLat", classLat);
                params.put("classLng", classLng);
                params.put("onSunday", onDays[0]);
                params.put("onMonday", onDays[1]);
                params.put("onTuesday", onDays[2]);
                params.put("onWednesday", onDays[3]);
                params.put("onThursday", onDays[4]);
                params.put("onFriday", onDays[5]);
                params.put("onSaturday", onDays[6]);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Function to store user in MySQL database will post params(tag, name,
     * email, password) to register url
     * */
    private void updateClass(final String userID, final String className, final String classTime, final String classLocation, final String[] onDays, final String classLat, final String classLng) {
        // Tag used to cancel the request
        String tag_string_req = "req_update_class";

        pDialog.setMessage("Updating Class...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_UPDATE_CLASS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Update Class Response: " + response.toString());
                hideDialog();

                inputClassName.setText("");
                inputClassTime.setText("");
                inputClassLocation.setText("");
                inputClassLat.setText("");
                inputClassLng.setText("");

                toggleSunday.setChecked(false);
                toggleMonday.setChecked(false);
                toggleTuesday.setChecked(false);
                toggleWednesday.setChecked(false);
                toggleThursday.setChecked(false);
                toggleFriday.setChecked(false);
                toggleSaturday.setChecked(false);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {

                        Toast.makeText(getApplicationContext(), "Class updated!", Toast.LENGTH_LONG).show();
                        setResult(Activity.RESULT_OK);
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
                Log.e(TAG, "Creation Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("classID", classID);
                params.put("userID", userID);
                params.put("accountID", accountID);
                params.put("className", className);
                params.put("classTime", classTime);
                params.put("classLocation", classLocation);
                params.put("classLat", classLat);
                params.put("classLng", classLng);
                params.put("onSunday", onDays[0]);
                params.put("onMonday", onDays[1]);
                params.put("onTuesday", onDays[2]);
                params.put("onWednesday", onDays[3]);
                params.put("onThursday", onDays[4]);
                params.put("onFriday", onDays[5]);
                params.put("onSaturday", onDays[6]);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
        Log.i(TAG, "Google Places API connected.");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Google Places API connection failed with error code: "
                + connectionResult.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int i) {
        mPlaceArrayAdapter.setGoogleApiClient(null);
        Log.e(TAG, "Google Places API connection suspended.");
    }
}
