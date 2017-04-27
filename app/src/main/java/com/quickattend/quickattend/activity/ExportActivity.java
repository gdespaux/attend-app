package com.quickattend.quickattend.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.CircularNetworkImageView;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ExportActivity extends AppCompatActivity {

    private static final String TAG = ExportActivity.class.getSimpleName();

    private ProgressDialog pDialog;
    private Menu menu;

    private EditText inputStartDate;
    private EditText inputEndDate;

    private SQLiteHandler db;
    private SessionManager session;
    private String userID;
    private String accountID;
    private String name;
    private String email;
    private String studentID;
    private String classID;
    private String className;
    private String studentName;

    private Calendar startDateCalendar = Calendar.getInstance();
    private Calendar endDateCalendar = Calendar.getInstance();
    DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            // TODO Auto-generated method stub
            startDateCalendar.set(Calendar.YEAR, year);
            startDateCalendar.set(Calendar.MONTH, monthOfYear);
            startDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateStartDate();
        }

    };

    DatePickerDialog.OnDateSetListener eDate = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            // TODO Auto-generated method stub
            endDateCalendar.set(Calendar.YEAR, year);
            endDateCalendar.set(Calendar.MONTH, monthOfYear);
            endDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateEndDate();
        }

    };

    String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        // Progress dialog
        pDialog = new ProgressDialog(ExportActivity.this);
        pDialog.setCancelable(false);

        inputStartDate = (EditText) findViewById(R.id.startDate);
        inputEndDate = (EditText) findViewById(R.id.endDate);

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

        studentID = getIntent().getStringExtra("studentID");
        classID = getIntent().getStringExtra("classID");
        className = getIntent().getStringExtra("className");
        studentName = getIntent().getStringExtra("studentName");

        inputStartDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    int year;
                    int month;
                    int day;

                    year = startDateCalendar.get(Calendar.YEAR);
                    month = startDateCalendar.get(Calendar.MONTH);
                    day = startDateCalendar.get(Calendar.DAY_OF_MONTH);

                    new DatePickerDialog(ExportActivity.this, date, year, month, day).show();
                }
            }
        });

        inputEndDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    int year;
                    int month;
                    int day;

                    year = endDateCalendar.get(Calendar.YEAR);
                    month = endDateCalendar.get(Calendar.MONTH);
                    day = endDateCalendar.get(Calendar.DAY_OF_MONTH);

                    new DatePickerDialog(ExportActivity.this, eDate, year, month, day).show();
                }
            }
        });

        new Instabug.Builder(getApplication(), AppConfig.INSTABUG_KEY)
                .setInvocationEvent(InstabugInvocationEvent.SHAKE)
                .build();
        Instabug.identifyUser(name, email);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_export_activity, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_export:
                if ((inputStartDate.getText().toString().matches("") && inputEndDate.getText().toString().matches("")) || (!inputStartDate.getText().toString().matches("") && !inputEndDate.getText().toString().matches(""))) {
                    exportClassAttendance();
                    return true;
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Invalid date range!", Toast.LENGTH_LONG)
                            .show();
                    return false;
                }

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void updateStartDate() {

        String myFormat = "MM/dd/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        selectedDate = sdf.format(startDateCalendar.getTime());

        inputStartDate.setText(selectedDate);
    }

    private void updateEndDate() {

        String myFormat = "MM/dd/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        selectedDate = sdf.format(endDateCalendar.getTime());

        inputEndDate.setText(selectedDate);
    }

    /**
     * Export class attendance from DB to user email
     */
    private void exportClassAttendance() {
        // Tag used to cancel the request
        String tag_string_req = "req_export_student_class_attendance";

        pDialog.setMessage("Exporting Attendance...");
        showDialog();

        final String startDate = inputStartDate.getText().toString().trim();
        final String endDate = inputEndDate.getText().toString().trim();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_EXPORT_STUDENT_CLASS_ATTENDANCE_BY_DATE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Export Attendance Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        hideDialog();
                        Toast.makeText(ExportActivity.this, "Attendance exported. Please check your email", Toast.LENGTH_LONG).show();
                        finish();
                    } else {

                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(ExportActivity.this,
                                errorMsg, Toast.LENGTH_LONG).show();
                        finish();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    finish();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Fetching Error: " + error.getMessage());
                Toast.makeText(ExportActivity.this,
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to export url
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountID", accountID);
                params.put("studentID", studentID);
                params.put("classID", classID);
                params.put("studentName", studentName);
                params.put("className", className);
                params.put("userEmail", email);
                params.put("userName", name);
                params.put("startDate", startDate);
                params.put("endDate", endDate);
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

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(ExportActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
