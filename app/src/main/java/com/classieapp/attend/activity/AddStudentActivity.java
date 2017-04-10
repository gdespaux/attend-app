package com.classieapp.attend.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Transition;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.classieapp.attend.R;
import com.classieapp.attend.adapters.AddStudentAdapter;
import com.classieapp.attend.app.AppConfig;
import com.classieapp.attend.app.AppController;
import com.classieapp.attend.utils.SQLiteHandler;
import com.classieapp.attend.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddStudentActivity extends AppCompatActivity {
    private static final String TAG = AddStudentActivity.class.getSimpleName();
    private ProgressDialog pDialog;
    private AutoCompleteTextView inputStudentName;
    private EditText inputStudentDOB;
    private EditText inputStudentPhone;
    private EditText inputStudentID;
    private EditText inputStudentEmail;
    private EditText inputStudentAddress;
    private EditText inputStudentEnrollDate;
    private EditText inputStudentMedInfo;
    private ImageButton studentPicture;

    private LinearLayout hideableInfo;

    private String studentGender;

    String currentDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String accountID;

    private boolean editMode = false;
    private boolean didSelectItem = false;

    List<String> studentList = new ArrayList<String>();

    private String classID;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private Calendar studentCalendar = Calendar.getInstance();
    DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            // TODO Auto-generated method stub
            studentCalendar.set(Calendar.YEAR, year);
            studentCalendar.set(Calendar.MONTH, monthOfYear);
            studentCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateStudentDOB();
        }

    };

    String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Transition fade = new Fade();
        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);

        Transition explode = new Explode();
        explode.excludeTarget(android.R.id.statusBarBackground, true);
        explode.excludeTarget(android.R.id.navigationBarBackground, true);

        // inside your activity (if you did not enable transitions in your theme)
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        // set an exit transition
        getWindow().setEnterTransition(explode);
        getWindow().setExitTransition(explode);
        getWindow().setAllowEnterTransitionOverlap(true);

        setContentView(R.layout.activity_add_student);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());

        // Fetching user details from sqlite
        final HashMap<String, String> user = db.getUserDetails();

        accountID = user.get("account_id");

        inputStudentDOB = (EditText) findViewById(R.id.studentDOB);
        inputStudentPhone = (EditText) findViewById(R.id.studentPhone);
        inputStudentID = (EditText) findViewById(R.id.studentID);
        inputStudentName = (AutoCompleteTextView)
                findViewById(R.id.studentName);
        studentPicture = (ImageButton) findViewById(R.id.studentPicture);

        inputStudentEmail = (EditText) findViewById(R.id.studentEmail);
        inputStudentAddress = (EditText) findViewById(R.id.studentAddress);
        inputStudentEnrollDate = (EditText) findViewById(R.id.studentEnrollDate);
        inputStudentMedInfo = (EditText) findViewById(R.id.studentMedInfo);

        hideableInfo = (LinearLayout) findViewById(R.id.hideableInfo);

        inputStudentPhone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        if(getIntent().hasExtra("editMode")){
            editMode = true;
            inputStudentName.setText(getIntent().getStringExtra("studentName"));
            inputStudentPhone.setText(getIntent().getStringExtra("studentPhone"));
            inputStudentID.setText(getIntent().getStringExtra("studentID"));

            studentGender = getIntent().getStringExtra("studentGender");

            String myFormat = "yyyy-MM-dd"; //In which you need put here
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

            if(!getIntent().getStringExtra("studentDOB").equals("0000-00-00")){
                try {
                    studentCalendar.setTime(sdf.parse(getIntent().getStringExtra("studentDOB")));
                    updateStudentDOB();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                inputStudentDOB.setText("");
            }

            ((RadioButton)findViewById(R.id.radioMale)).setChecked(getIntent().getStringExtra("studentGender").equals("Male"));
            ((RadioButton)findViewById(R.id.radioFemale)).setChecked(getIntent().getStringExtra("studentGender").equals("Female"));

        } else {
            //currentDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());
        }

        studentPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(AddStudentActivity.this);
//                builder.setTitle("Change Photo") //
//                        .setMessage("Pick a picture!")
//                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                // TODO
//                                dialog.dismiss();
//                            }
//                        });
//                builder.show();

                //Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                //    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                //}

            }
        });

        inputStudentName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                didSelectItem = false;
                hideableInfo.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        inputStudentDOB.setOnClickListener(new EditText.OnClickListener() {

            @Override
            public void onClick(View v) {

                int year;
                int month;
                int day;

/*
                if(editMode){
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                    Date date = null;
                    try {
                        date = sdf.parse(inputStudentDOB.getText().toString());

                        if(date == null){
                            date = sdf.parse(currentDate);
                        }
                    } catch (ParseException e) {
                    }
                    Calendar c = Calendar.getInstance();
                    c.setTime(date);

                    year = c.get(Calendar.YEAR);
                    month = c.get(Calendar.MONTH);
                    day = c.get(Calendar.DAY_OF_MONTH);
                } else {
                    year = studentCalendar.get(Calendar.YEAR);
                    month = studentCalendar.get(Calendar.MONTH);
                    day = studentCalendar.get(Calendar.DAY_OF_MONTH);
                }
*/
                year = studentCalendar.get(Calendar.YEAR);
                month = studentCalendar.get(Calendar.MONTH);
                day = studentCalendar.get(Calendar.DAY_OF_MONTH);

                new DatePickerDialog(AddStudentActivity.this, date, year, month, day).show();

            }
        });

        classID = getIntent().getStringExtra("classID");

        getTypeaheadStudents();

    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radioMale:
                if (checked)
                    studentGender = "Male";
                break;
            case R.id.radioFemale:
                if (checked)
                    studentGender = "Female";
                break;
        }
    }

    private void updateStudentDOB() {

        String myFormat = "MM/dd/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        selectedDate = sdf.format(studentCalendar.getTime());

        inputStudentDOB.setText(selectedDate);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            studentPicture.setImageBitmap(imageBitmap);
        }
    }

    /**
     * Function to get all of user's classes from MySQL DB
     * */
    private void getTypeaheadStudents() {
        // Tag used to cancel the request
        String tag_string_req = "req_get_all_classes";

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_TYPEAHEAD_ACCOUNT_STUDENTS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Get Students Response: " + response.toString());
                ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String, String>>();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        JSON_STRING = response;
                        JSONArray result = jObj.getJSONArray("students");

                        for(int i = 0; i<result.length(); i++){
                            JSONObject jo = result.getJSONObject(i);
                            String studentName = jo.getString("studentName");
                            String studentAge = jo.getString("studentAge");
                            String studentID = jo.getString("studentID");

                            HashMap<String,String> student = new HashMap<>();
                            student.put("studentName",studentName);
                            student.put("studentAge", studentAge);
                            student.put("studentID", studentID);
                            list.add(student);
                        }

                        AddStudentAdapter adapter = new AddStudentAdapter(
                                AddStudentActivity.this, list,
                                new String[]{"studentName", "studentAge", "studentID"},
                                new int[]{R.id.studentName, R.id.studentAge, R.id.studentID});

                        inputStudentName.setAdapter(adapter);
                        inputStudentName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                HashMap<String,String> student = (HashMap<String,String>) parent.getItemAtPosition(position);

                                Log.i("FROMARRAY", student.get("studentName"));
                                inputStudentName.setText(student.get("studentName"));
                                inputStudentID.setText(student.get("studentID"));

                                didSelectItem = true;
                                hideableInfo.setVisibility(View.GONE);
                            }
                        });

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
                params.put("accountID", accountID);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(editMode){
            getMenuInflater().inflate(R.menu.menu_update_student, menu);
        } else{
            getMenuInflater().inflate(R.menu.menu_add_student, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        String studentName = inputStudentName.getText().toString().trim();
        String studentBirthday = inputStudentDOB.getText().toString().trim();
        String studentID = inputStudentID.getText().toString().trim();
        String studentPhone = inputStudentPhone.getText().toString().trim();

        switch (item.getItemId()) {
            case R.id.action_add_student:

                if (!studentName.isEmpty()) {
                    addStudent(classID, studentName, studentBirthday, studentPhone, studentID);

                    return true;
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Student name is required!", Toast.LENGTH_LONG)
                            .show();
                    return false;
                }

            case R.id.action_update_student:

                if (!studentName.isEmpty()) {
                    updateStudent(classID, studentName, studentBirthday, studentPhone, studentID);

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
     * age, existing id) to add student url
     * */
    private void addStudent(final String classID, final String studentName, final String studentDOB, final String studentPhone, final String studentID) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_student";

        pDialog.setMessage("Adding Student...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_ADD_STUDENT, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Add Student Response: " + response.toString());
                hideDialog();

                inputStudentName.setText("");
                inputStudentDOB.setText("");
                inputStudentID.setText("");
                inputStudentPhone.setText("");

                ((RadioButton)findViewById(R.id.radioMale)).setChecked(false);
                ((RadioButton)findViewById(R.id.radioFemale)).setChecked(false);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        Toast.makeText(getApplicationContext(), "Student added!", Toast.LENGTH_LONG).show();
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
                params.put("accountID", accountID);
                params.put("studentName", studentName);
                params.put("studentDOB", studentDOB);
                params.put("studentPhone", studentPhone);
                params.put("studentGender", studentGender);
                params.put("studentID", studentID);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Function to update student in MySQL database will post params(class, name,
     * age, existing id) to update student url
     * */
    private void updateStudent(final String classID, final String studentName, final String studentDOB, final String studentPhone, final String studentID) {
        // Tag used to cancel the request
        String tag_string_req = "req_update_student";

        pDialog.setMessage("Updating Student...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_UPDATE_STUDENT, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Update Student Response: " + response.toString());
                hideDialog();

                inputStudentName.setText("");
                inputStudentDOB.setText("");
                inputStudentID.setText("");
                inputStudentPhone.setText("");

                ((RadioButton)findViewById(R.id.radioMale)).setChecked(false);
                ((RadioButton)findViewById(R.id.radioFemale)).setChecked(false);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        Toast.makeText(getApplicationContext(), "Student updated!", Toast.LENGTH_LONG).show();
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
                params.put("accountID", accountID);
                params.put("studentID", studentID);
                params.put("studentName", studentName);
                params.put("studentDOB", studentDOB);
                params.put("studentPhone", studentPhone);
                params.put("studentGender", studentGender);

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
