package com.quickattend.quickattend.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
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
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.CircularNetworkImageView;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private TextView studentEmailText;
    private TextView studentAddressText;
    private TextView studentEnrollDateText;
    private TextView studentMedInfoText;
    private TextView studentLastSeenText;
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
    private String studentEmail;
    private String studentAddress;
    private String studentEnrollDate;
    private String studentMedInfo;
    private static final int STATIC_RESULT = 4;

    ImageLoader imageLoader = AppController.getInstance().getImageLoader();
    private Calendar studentCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_student);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        studentNameText = (TextView) findViewById(R.id.studentName);
        studentDOBText = (TextView) findViewById(R.id.studentDOB);
        studentAgeText = (TextView) findViewById(R.id.studentAge);
        studentPhoneText = (TextView) findViewById(R.id.studentPhone);
        studentGenderText = (TextView) findViewById(R.id.studentGender);
        studentEmailText = (TextView) findViewById(R.id.studentEmail);
        studentAddressText = (TextView) findViewById(R.id.studentAddress);
        studentEnrollDateText = (TextView) findViewById(R.id.studentEnrollDate);
        studentMedInfoText = (TextView) findViewById(R.id.studentMedInfo);
        studentLastSeenText = (TextView) findViewById(R.id.studentLastSeen);
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

        new Instabug.Builder(getApplication(), AppConfig.INSTABUG_KEY)
                .setInvocationEvent(InstabugInvocationEvent.SHAKE)
                .build();
        Instabug.identifyUser(email, email);

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
                intent.putExtra("studentEmail", studentEmail);
                intent.putExtra("studentAddress", studentAddress);
                intent.putExtra("studentEnrollDate", studentEnrollDate);
                intent.putExtra("studentMedInfo", studentMedInfo);
                startActivityForResult(intent, STATIC_RESULT);

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
            String studentEmail = jo.getString("studentEmail");
            String studentAddress = jo.getString("studentAddress");
            String studentEnrollDate = jo.getString("studentEnrollDate");
            String studentMedInfo = jo.getString("studentMedInfo");
            String lastClass = jo.getString("studentLastClass");
            String lastClassDate = jo.getString("studentLastAttendance");

            getSupportActionBar().setTitle(studentName);

            this.studentName = studentName;
            this.studentDOB = studentDOB;
            Log.i("DOB", studentDOB);
            this.studentPhone = studentPhone;
            this.studentGender = studentGender;
            this.studentEmail = studentEmail;
            this.studentAddress = studentAddress;
            this.studentEnrollDate = studentEnrollDate;
            this.studentMedInfo = studentMedInfo;

            String myFormat = "yyyy-MM-dd"; //In which you need put here
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
            String newFormat = "MM/dd/yyyy"; //In which you need put here
            SimpleDateFormat newSdf = new SimpleDateFormat(newFormat, Locale.US);

            try{
                studentLastSeenText.setText("Last seen\n" + newSdf.format(sdf.parse(lastClassDate)) + " in " + lastClass);
            } catch(ParseException e){
                studentLastSeenText.setText("No classes attended!");
            }

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

            if(!studentEnrollDate.equals("0000-00-00")){
                studentEnrollDateText.setText(newSdf.format(sdf.parse(studentEnrollDate)));
            } else {
                studentEnrollDateText.setText("");
            }

            studentNameText.setText(studentName);
            //studentDOBText.setText(studentDOB);
            studentPhoneText.setText(studentPhone);
            studentGenderText.setText(studentGender);
            studentEmailText.setText(studentEmail);
            studentAddressText.setText(studentAddress);
            studentMedInfoText.setText(studentMedInfo);
            this.studentPhoto.setImageUrl(studentPhoto, imageLoader);

            if(studentEmailText.getText().equals("")) studentEmailText.setVisibility(View.GONE);
            if(studentAddressText.getText().equals("")) studentAddressText.setVisibility(View.GONE);
            if(studentEnrollDateText.getText().equals("")) studentEnrollDateText.setVisibility(View.GONE);
            if(studentMedInfoText.getText().equals("")) studentMedInfoText.setVisibility(View.GONE);

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
