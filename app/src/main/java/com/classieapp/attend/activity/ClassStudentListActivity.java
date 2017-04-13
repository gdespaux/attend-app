package com.classieapp.attend.activity;

import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.classieapp.attend.R;
import com.classieapp.attend.adapters.StudentListAdapter;
import com.classieapp.attend.app.AppConfig;
import com.classieapp.attend.app.AppController;
import com.classieapp.attend.utils.SQLiteHandler;
import com.classieapp.attend.utils.SessionManager;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ClassStudentListActivity extends AppCompatActivity implements ListView.OnItemClickListener, ListView.OnItemLongClickListener {
    private static final String TAG = ClassStudentListActivity.class.getSimpleName();
    private ListView listView;
    private ProgressDialog pDialog;

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String classID;
    private String className;
    private String accountID;
    private String email;

    private Calendar studentCalendar = Calendar.getInstance();
    DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            // TODO Auto-generated method stub
            studentCalendar.set(Calendar.YEAR, year);
            studentCalendar.set(Calendar.MONTH, monthOfYear);
            studentCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateStudentDate();
        }

    };

    String currentDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_student_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        className = getIntent().getStringExtra("className");

        if(getIntent().hasExtra("classDate")){
            currentDate = getIntent().getStringExtra("classDate");
        } else {
            currentDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());
        }

        getSupportActionBar().setTitle(className + ": " + currentDate);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launching the add class activity
                Intent intent = new Intent(ClassStudentListActivity.this, AddStudentActivity.class);
                intent.putExtra("classID",classID);
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(ClassStudentListActivity.this).toBundle());
            }
        });
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // Fetching user details from sqlite
        final HashMap<String, String> user = db.getUserDetails();

        accountID = user.get("account_id");
        email = user.get("email");

        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        classID = getIntent().getStringExtra("classID");
        getClassStudents(classID);

        new Instabug.Builder(getApplication(), AppConfig.INSTABUG_KEY)
                .setInvocationEvent(InstabugInvocationEvent.SHAKE)
                .build();
        Instabug.identifyUser(email, email);
    }

    private void updateStudentDate() {

        String myFormat = "MM/dd/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        currentDate = sdf.format(studentCalendar.getTime());

        getSupportActionBar().setTitle("Today's Attendance: " + currentDate);
        getClassStudents(classID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_class_student_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_change_date) {
            new DatePickerDialog(ClassStudentListActivity.this, date, studentCalendar
                    .get(Calendar.YEAR), studentCalendar.get(Calendar.MONTH),
                    studentCalendar.get(Calendar.DAY_OF_MONTH)).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Intent intent = new Intent(this, ViewEmployee.class);
        HashMap<String,String> map =(HashMap)parent.getItemAtPosition(position);
        String studentID = map.get("studentID").toString();
        //intent.putExtra("classID",empId);
        //startActivity(intent);

        CheckBox studentPresent = (CheckBox) view.findViewById(R.id.studentPresent);
        Boolean presentNow = !studentPresent.isChecked();
        studentPresent.setChecked(presentNow);

        addStudentAttendance(studentID, presentNow);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        Toast.makeText(getApplicationContext(),
                "Can't edit yet!", Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getClassStudents(classID);
    }

    private void showStudent(){
        JSONObject jsonObject = null;
        ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String, String>>();
        try {
            jsonObject = new JSONObject(JSON_STRING);
            JSONArray result = jsonObject.getJSONArray("students");

            for(int i = 0; i<result.length(); i++){
                JSONObject jo = result.getJSONObject(i);
                String studentID = jo.getString("studentID");
                String studentName = jo.getString("studentName");
                String studentPresent = jo.getString("studentPresent");
                String studentPhoto = jo.getString("studentPhoto");
                String studentActive = jo.getString("studentActive");
                String studentInactiveDate = jo.getString("inactiveDate");

                if(!studentActive.equals("yes")) continue;

                HashMap<String,String> student = new HashMap<>();
                student.put("studentID",studentID);
                student.put("studentName", studentName);
                student.put("studentPresent",studentPresent);
                student.put("studentPhoto",studentPhoto);
                list.add(student);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        ListAdapter adapter = new StudentListAdapter(
                ClassStudentListActivity.this, list,
                new String[]{"studentID", "studentName", "studentPresent", "studentPhoto"},
                new int[]{R.id.studentID, R.id.studentName, R.id.studentPresent, R.id.studentPhoto});

        listView.setAdapter(adapter);
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     * */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(ClassStudentListActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Function to get all of user's classes from MySQL DB
     * */
    private void addStudentAttendance(final String studentID, final Boolean studentPresent) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_attendance";

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_ADD_ATTENDANCE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Add Attendance Response: " + response.toString());

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
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

            String studentAttend = "";

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                if(studentPresent){
                    studentAttend = "yes";
                } else {
                    studentAttend = "no";
                }

                Map<String, String> params = new HashMap<String, String>();
                params.put("classID", classID);
                params.put("accountID", accountID);
                params.put("studentID", studentID);
                params.put("studentPresent", studentAttend);
                params.put("classDate", currentDate);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Function to get all of user's classes from MySQL DB
     * */
    private void getClassStudents(final String classID) {
        // Tag used to cancel the request
        String tag_string_req = "req_get_class_students";

        pDialog.setMessage("Loading Students...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_GET_CLASS_STUDENTS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Get Students Response: " + response.toString());
                hideDialog();

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
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("classID", classID);
                params.put("classDate", currentDate);
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
