package com.quickattend.quickattend.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddLeadActivity extends AppCompatActivity {
    private static final String TAG = LeadFragment.class.getSimpleName();
    private ProgressDialog pDialog;
    private EditText inputLeadID;
    private EditText inputStudentName;
    private EditText inputStudentDOB;
    private EditText inputStudentPhone;
    private EditText inputStudentEmail;
    private EditText inputStudentAddress;
    private EditText inputStudentMiscInfo;

    private RadioGroup radioGroup;
    private Button submitButton;

    private SQLiteHandler db;
    private SessionManager session;
    private String userID;
    private String accountID;

    private boolean editMode = false;

    private Calendar studentDOBCalendar = Calendar.getInstance();
    DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            // TODO Auto-generated method stub
            studentDOBCalendar.set(Calendar.YEAR, year);
            studentDOBCalendar.set(Calendar.MONTH, monthOfYear);
            studentDOBCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateStudentDOB();
        }

    };

    String selectedDate;
    private String studentGender = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lead);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

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
        String name = user.get("name");
        String email = user.get("email");

        inputLeadID = (EditText) findViewById(R.id.leadID);
        inputStudentDOB = (EditText) findViewById(R.id.studentDOB);
        inputStudentPhone = (EditText) findViewById(R.id.studentPhone);
        inputStudentName = (EditText) findViewById(R.id.studentName);

        inputStudentEmail = (EditText) findViewById(R.id.studentEmail);
        inputStudentAddress = (EditText) findViewById(R.id.studentAddress);
        inputStudentMiscInfo = (EditText) findViewById(R.id.studentMiscInfo);

        radioGroup = (RadioGroup) findViewById(R.id.genderRadios);

        submitButton = (Button) findViewById(R.id.submitButton);

        inputStudentPhone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        if (getIntent().hasExtra("editMode")) {
            editMode = true;
            inputStudentName.setText(getIntent().getStringExtra("studentName"));
            inputStudentPhone.setText(getIntent().getStringExtra("studentPhone"));
            inputStudentEmail.setText(getIntent().getStringExtra("studentEmail"));
            inputStudentAddress.setText(getIntent().getStringExtra("studentAddress"));
            inputStudentMiscInfo.setText(getIntent().getStringExtra("studentMiscInfo"));

            inputLeadID.setText(getIntent().getStringExtra("leadID"));

            studentGender = getIntent().getStringExtra("studentGender");
            if(studentGender == null){
                studentGender = "";
            }

            String myFormat = "yyyy-MM-dd"; //In which you need put here
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

            if (!getIntent().getStringExtra("studentDOB").equals("0000-00-00")) {
                try {
                    studentDOBCalendar.setTime(sdf.parse(getIntent().getStringExtra("studentDOB")));
                    updateStudentDOB();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                inputStudentDOB.setText("");
            }

            ((RadioButton) findViewById(R.id.radioMale)).setChecked(getIntent().getStringExtra("studentGender").equals("Male"));
            ((RadioButton) findViewById(R.id.radioFemale)).setChecked(getIntent().getStringExtra("studentGender").equals("Female"));

        }

        inputStudentDOB.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    int year;
                    int month;
                    int day;

                    year = studentDOBCalendar.get(Calendar.YEAR);
                    month = studentDOBCalendar.get(Calendar.MONTH);
                    day = studentDOBCalendar.get(Calendar.DAY_OF_MONTH);

                    new DatePickerDialog(AddLeadActivity.this, date, year, month, day).show();
                }
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                // Check which radio button was clicked
                switch (checkedId) {
                    case R.id.radioMale:
                        studentGender = "Male";
                        break;
                    case R.id.radioFemale:
                        studentGender = "Female";
                        break;
                }
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String studentName = inputStudentName.getText().toString().trim();
                String studentBirthday = inputStudentDOB.getText().toString().trim();
                String studentPhone = inputStudentPhone.getText().toString().trim();
                String studentEmail = inputStudentEmail.getText().toString().trim();
                String studentAddress = inputStudentAddress.getText().toString().trim();
                String studentMedInfo = inputStudentMiscInfo.getText().toString().trim();

                if (!studentName.isEmpty()) {
                    addLead(studentName, studentBirthday, studentPhone, studentEmail, studentAddress, studentMedInfo);
                } else {
                    Toast.makeText(AddLeadActivity.this,
                            "Student name is required!", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        new Instabug.Builder(getApplication(), AppConfig.INSTABUG_KEY)
                .setInvocationEvent(InstabugInvocationEvent.SHAKE)
                .build();
        Instabug.identifyUser(name, email);

    }

    private void updateStudentDOB() {

        String myFormat = "MM/dd/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        selectedDate = sdf.format(studentDOBCalendar.getTime());

        inputStudentDOB.setText(selectedDate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (editMode) {
            getMenuInflater().inflate(R.menu.menu_update_lead, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_add_lead, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        String leadID = inputLeadID.getText().toString().trim();
        String studentName = inputStudentName.getText().toString().trim();
        String studentBirthday = inputStudentDOB.getText().toString().trim();
        String studentPhone = inputStudentPhone.getText().toString().trim();
        String studentEmail = inputStudentEmail.getText().toString().trim();
        String studentAddress = inputStudentAddress.getText().toString().trim();
        String studentMedInfo = inputStudentMiscInfo.getText().toString().trim();

        switch (item.getItemId()) {
            case R.id.action_add_lead:

                if (!studentName.isEmpty()) {

                    addLead(studentName, studentBirthday, studentPhone, studentEmail, studentAddress, studentMedInfo);

                    return true;
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Student name is required!", Toast.LENGTH_LONG)
                            .show();
                    return false;
                }

            case R.id.action_update_lead:

                if (!studentName.isEmpty()) {
                    updateLead(studentName, studentBirthday, studentPhone, studentEmail, studentAddress, studentMedInfo, leadID);

                    return true;
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Student name is required!", Toast.LENGTH_LONG)
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
     * Function to store student in MySQL database will post params(class, name,
     * age, existing id) to add lead url
     */
    private void addLead(final String studentName, final String studentDOB, final String studentPhone, final String studentEmail, final String studentAddress, final String studentMiscInfo) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_lead";

        pDialog.setMessage("Adding Lead...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_ADD_LEAD, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Add Lead Response: " + response);
                hideDialog();

                inputStudentName.setText("");
                inputStudentDOB.setText("");
                inputStudentPhone.setText("");
                inputStudentEmail.setText("");
                inputStudentAddress.setText("");
                inputStudentMiscInfo.setText("");

                ((RadioButton) findViewById(R.id.radioMale)).setChecked(false);
                ((RadioButton) findViewById(R.id.radioFemale)).setChecked(false);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        Toast.makeText(AddLeadActivity.this, "Lead info added!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(AddLeadActivity.this,
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Submission Error: " + error.getMessage());
                Toast.makeText(AddLeadActivity.this,
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {


                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountID", accountID);
                params.put("studentName", studentName);
                params.put("studentDOB", studentDOB);
                params.put("studentPhone", studentPhone);
                params.put("studentGender", studentGender);
                params.put("studentEmail", studentEmail);
                params.put("studentAddress", studentAddress);
                params.put("studentMiscInfo", studentMiscInfo);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Function to update student in MySQL database will post params(class, name,
     * age, existing id) to update student url
     */
    private void updateLead(final String studentName, final String studentDOB, final String studentPhone, final String studentEmail, final String studentAddress, final String studentMiscInfo, final String leadID) {
        // Tag used to cancel the request
        String tag_string_req = "req_update_lead";

        pDialog.setMessage("Updating Lead...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_UPDATE_LEAD, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Update Lead Response: " + response);
                hideDialog();

                inputStudentName.setText("");
                inputStudentDOB.setText("");
                inputLeadID.setText("");
                inputStudentPhone.setText("");
                inputStudentEmail.setText("");
                inputStudentAddress.setText("");
                inputStudentMiscInfo.setText("");

                ((RadioButton) findViewById(R.id.radioMale)).setChecked(false);
                ((RadioButton) findViewById(R.id.radioFemale)).setChecked(false);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        Toast.makeText(getApplicationContext(), "Lead updated!", Toast.LENGTH_LONG).show();
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

                Log.e("LEADID", "UPDATING");

                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountID", accountID);
                params.put("leadID", leadID);
                params.put("studentName", studentName);
                params.put("studentDOB", studentDOB);
                params.put("studentPhone", studentPhone);
                params.put("studentGender", studentGender);
                params.put("studentEmail", studentEmail);
                params.put("studentAddress", studentAddress);
                params.put("studentMiscInfo", studentMiscInfo);

                Log.e("LEADID", leadID);

                return params;
            }

        };

        // Adding request to request queue
        Log.e("LEADID", "UPDATINGNOW");
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }


    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     * */
    private void logoutUser() {
        session.setLogin(false);
        session.enterLeadMode(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(AddLeadActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
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
