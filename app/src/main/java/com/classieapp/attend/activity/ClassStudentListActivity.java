package com.classieapp.attend.activity;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Explode;
import android.transition.Fade;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClassStudentListActivity extends AppCompatActivity implements ListView.OnItemClickListener {
    private static final String TAG = ClassStudentListActivity.class.getSimpleName();
    private ListView listView;
    private ProgressDialog pDialog;

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String classID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inside your activity (if you did not enable transitions in your theme)
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        // set an exit transition
        getWindow().setEnterTransition(new Explode());
        getWindow().setExitTransition(new Explode());
        getWindow().setAllowEnterTransitionOverlap(true);

        setContentView(R.layout.activity_class_student_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        classID = getIntent().getStringExtra("classID");
        getClassStudents(classID);
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
                //String studentPresent = jo.getString("studentPresent");
                String studentPresent = "";

                HashMap<String,String> student = new HashMap<>();
                student.put("studentID",studentID);
                student.put("studentName", studentName);
                student.put("studentPresent",studentPresent);
                list.add(student);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        ListAdapter adapter = new SimpleAdapter(
                ClassStudentListActivity.this, list, R.layout.student_list_item,
                new String[]{"studentID", "studentName", "studentPresent"},
                new int[]{R.id.studentID, R.id.studentName, R.id.studentPresent});

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
                params.put("studentID", studentID);
                params.put("studentPresent", studentAttend);
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
