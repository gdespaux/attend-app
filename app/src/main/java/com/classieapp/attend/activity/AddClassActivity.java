package com.classieapp.attend.activity;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.classieapp.attend.R;
import com.classieapp.attend.app.AppConfig;
import com.classieapp.attend.app.AppController;
import com.classieapp.attend.utils.SQLiteHandler;
import com.classieapp.attend.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddClassActivity extends AppCompatActivity {
    private static final String TAG = AddClassActivity.class.getSimpleName();
    private Button btnAddClass;
    private ProgressDialog pDialog;
    private EditText inputClassName;
    private EditText inputClassTime;
    private EditText inputClassLocation;

    private SQLiteHandler db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        btnAddClass = (Button) findViewById(R.id.btnAddClass);
        inputClassName = (EditText) findViewById(R.id.className);
        inputClassTime = (EditText) findViewById(R.id.classTime);
        inputClassLocation = (EditText) findViewById(R.id.classLocation);

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        // Fetching user details from sqlite
        final HashMap<String, String> user = db.getUserDetails();

        // View classes click event
        btnAddClass.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String userID = user.get("uid");
                String className = inputClassName.getText().toString().trim();
                String classTime = inputClassTime.getText().toString();
                String classLocation = inputClassLocation.getText().toString().trim();

                if (!className.isEmpty() && !classTime.isEmpty() && !classLocation.isEmpty()) {
                    addClass(userID, className, classTime, classLocation);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Please enter class details!", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        inputClassTime.setOnClickListener(new EditText.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(AddClassActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
                        int hour = hourOfDay % 12;
                        if(hour == 0){
                            hour = 12;
                        }
                        inputClassTime.setText(String.format(Locale.US, "%02d:%02d %s", hour, minute,
                                hourOfDay < 12 ? "am" : "pm"));
                    }
                }, hour, minute, false);//Yes 24 hour time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();

            }
        });
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
    private void addClass(final String userID, final String className, final String classTime, final String classLocation) {
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
                params.put("className", className);
                params.put("classTime", classTime);
                params.put("classLocation", classLocation);

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

}
