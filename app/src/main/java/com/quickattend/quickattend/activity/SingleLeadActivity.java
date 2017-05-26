package com.quickattend.quickattend.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.CircularNetworkImageView;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;

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

public class SingleLeadActivity extends AppCompatActivity {
    private static final String TAG = SingleLeadActivity.class.getSimpleName();

    private ProgressDialog pDialog;

    private Menu menu;

    private TextView studentNameText;
    private TextView studentDOBText;
    private TextView studentAgeText;
    private TextView studentPhoneText;
    private TextView studentGenderText;
    private TextView studentEmailText;
    private TextView studentAddressText;
    private TextView studentMiscInfoText;

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String userID;
    private String accountID;
    private String name;
    private String email;

    private String leadID;
    private String studentName;
    private String studentDOB;
    private String studentPhone;
    private String studentGender;
    private String studentEmail;
    private String studentAddress;
    private String studentMiscInfo;
    private static final int STATIC_RESULT = 4;

    private Calendar studentCalendar = Calendar.getInstance();

    private ArrayList<String> dialogItems = new ArrayList<String>();
    private ArrayList<String> dialogItemsMap = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_lead);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Progress dialog
        pDialog = new ProgressDialog(SingleLeadActivity.this);
        pDialog.setCancelable(false);

        studentNameText = (TextView) findViewById(R.id.studentName);
        studentDOBText = (TextView) findViewById(R.id.studentDOB);
        studentAgeText = (TextView) findViewById(R.id.studentAge);
        studentPhoneText = (TextView) findViewById(R.id.studentPhone);
        studentGenderText = (TextView) findViewById(R.id.studentGender);
        studentEmailText = (TextView) findViewById(R.id.studentEmail);
        studentAddressText = (TextView) findViewById(R.id.studentAddress);
        studentMiscInfoText = (TextView) findViewById(R.id.studentMiscInfo);

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

        leadID = getIntent().getStringExtra("leadID");
        getSingleLead();

        new Instabug.Builder(getApplication(), AppConfig.INSTABUG_KEY)
                .setInvocationEvent(InstabugInvocationEvent.SHAKE)
                .build();
        Instabug.identifyUser(name, email);

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
        getMenuInflater().inflate(R.menu.menu_single_lead, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_update_lead:

                // Launching the add lead activity
                Intent intent = new Intent(SingleLeadActivity.this, AddLeadActivity.class);
                intent.putExtra("editMode", true);
                intent.putExtra("leadID", leadID);
                intent.putExtra("studentName", studentName);
                intent.putExtra("studentDOB", studentDOB);
                intent.putExtra("studentPhone", studentPhone);
                intent.putExtra("studentGender", studentGender);
                intent.putExtra("studentEmail", studentEmail);
                intent.putExtra("studentAddress", studentAddress);
                intent.putExtra("studentMiscInfo", studentMiscInfo);
                startActivityForResult(intent, STATIC_RESULT);

                return true;

            case R.id.action_delete_lead:
                AlertDialog.Builder builder = new AlertDialog.Builder(SingleLeadActivity.this);
                builder.setTitle("Delete Lead")
                        .setMessage("This will delete the lead! Are you sure?")
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteStudent();
                                dialog.dismiss();
                            }
                        });
                builder.show();

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

            JSONObject jo = jsonObject.getJSONObject("lead");
            //JSONObject cdObject = jo.getJSONObject("classDays");

            String studentName = jo.getString("studentName");
            String studentDOB = jo.getString("studentDOB");
            String studentPhone = jo.getString("studentPhone");
            String studentGender = jo.getString("studentGender");
            String studentEmail = jo.getString("studentEmail");
            String studentAddress = jo.getString("studentAddress");
            String studentMiscInfo = jo.getString("studentMiscInfo");

            getSupportActionBar().setTitle(studentName);

            this.studentName = studentName;
            this.studentDOB = studentDOB;
            this.studentPhone = studentPhone;
            this.studentGender = studentGender;
            this.studentEmail = studentEmail;
            this.studentAddress = studentAddress;
            this.studentMiscInfo = studentMiscInfo;

            String myFormat = "yyyy-MM-dd";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
            String newFormat = "MM/dd/yyyy";
            SimpleDateFormat newSdf = new SimpleDateFormat(newFormat, Locale.US);

            studentCalendar.setTime(sdf.parse(studentDOB));
            int year = studentCalendar.get(Calendar.YEAR);
            int month = studentCalendar.get(Calendar.MONTH);
            int day = studentCalendar.get(Calendar.DAY_OF_MONTH);

            if(!studentDOB.equals("0000-00-00")){
                studentDOBText.setText("DOB: " + newSdf.format(sdf.parse(studentDOB)));
                studentAgeText.setText("Age: " + getAge(year, month, day));
            } else {
                studentDOBText.setText("");
                studentAgeText.setText("");
            }

            studentNameText.setText(studentName);
            studentPhoneText.setText("Phone: " + studentPhone);
            studentGenderText.setText(studentGender);
            studentEmailText.setText("Email: " + studentEmail);
            studentAddressText.setText("Address: " + studentAddress);
            studentMiscInfoText.setText("Misc Info: " + studentMiscInfo);

            if(studentEmailText.getText().equals("")) studentEmailText.setVisibility(View.GONE);
            if(studentAddressText.getText().equals("")) studentAddressText.setVisibility(View.GONE);
            if(studentMiscInfoText.getText().equals("")) studentMiscInfoText.setVisibility(View.GONE);

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    /**
     * Function to set selected student deleted from MySQL DB
     */
    private void deleteStudent(){
        // Tag used to cancel the request
        String tag_string_req = "req_delete_lead";


        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_DELETE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Delete Lead Response: " + response);

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
                params.put("leadID", leadID);
                params.put("deleteType", "leads");
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Function to get selected lead from MySQL DB
     */
    private void getSingleLead() {
        // Tag used to cancel the request
        String tag_string_req = "req_get_single_lead";


        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_GET_SINGLE_LEAD, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Get Lead Response: " + response);

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
                params.put("leadID", leadID);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {

            case STATIC_RESULT:
                if(resultCode == Activity.RESULT_OK){
                    getSingleLead();
                }
                break;

        }
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
        Intent intent = new Intent(SingleLeadActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

}
