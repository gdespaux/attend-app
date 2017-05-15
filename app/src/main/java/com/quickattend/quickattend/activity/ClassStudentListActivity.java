package com.quickattend.quickattend.activity;
/*
Lists class students to take attendance.
Called when class clicked on home screen
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.models.Student;
import com.quickattend.quickattend.adapters.StudentListAdapter;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.NotificationUtils;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassStudentListActivity extends AppCompatActivity {
    private static final String TAG = ClassStudentListActivity.class.getSimpleName();
    private RecyclerView listView;
    private ProgressDialog pDialog;

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String classID;
    private String className;
    private String accountID;
    private String email;

    private BroadcastReceiver mRegistrationBroadcastReceiver;

    private List<Student> students;
    private StudentListAdapter adapter;

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

    FloatingActionButton fab, fabExistingStudent, fabNewStudent;
    LinearLayout fabLayout1, fabLayout2;
    View fabBGLayout;
    boolean isFABOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_student_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        className = getIntent().getStringExtra("className");

        if (getIntent().hasExtra("classDate")) {
            currentDate = getIntent().getStringExtra("classDate");
        } else {
            currentDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());
        }

        getSupportActionBar().setTitle(className + ": " + currentDate);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fabLayout1 = (LinearLayout) findViewById(R.id.fabLayout1);
        fabLayout2 = (LinearLayout) findViewById(R.id.fabLayout2);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabExistingStudent = (FloatingActionButton) findViewById(R.id.fab1);
        fabNewStudent = (FloatingActionButton) findViewById(R.id.fab2);
        fabBGLayout = findViewById(R.id.fabBGLayout);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isFABOpen) {
                    showFABMenu();
                } else {
                    closeFABMenu();
                }
            }
        });

        fabExistingStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launching the add class activity
                closeFABMenu();
                Intent intent = new Intent(ClassStudentListActivity.this, AddStudentActivity.class);
                intent.putExtra("classID", classID);
                intent.putExtra("studentOnly", "yes");
                startActivity(intent);
            }
        });

        fabNewStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launching the add class activity
                closeFABMenu();
                Intent intent = new Intent(ClassStudentListActivity.this, AddStudentActivity.class);
                intent.putExtra("classID", classID);
                startActivity(intent);
            }
        });

        fabBGLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeFABMenu();
            }
        });

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // Fetching user details from sqlite
        final HashMap<String, String> user = db.getUserDetails();

        accountID = user.get("account_id");
        String name = user.get("name");
        email = user.get("email");

        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        classID = getIntent().getStringExtra("classID");

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        listView = (RecyclerView) findViewById(R.id.listView);
        listView.setHasFixedSize(true);
        listView.setLayoutManager(layoutManager);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction().equals(AppConfig.PUSH_NOTIFICATION)) {
                    // new push notification is received
                    String message = intent.getStringExtra("message");
                    String payload = intent.getStringExtra("payload");

                    try {
                        JSONObject jsonObject = new JSONObject(payload);
                        String studentName = jsonObject.getString("studentName");

                        //Toast.makeText(getApplicationContext(), "New student: " + studentName, Toast.LENGTH_LONG).show();
                        Student student = new Student("1000", studentName, "http://quickattendapp.com/documents/stock.png", "yes", "37", "58f623fd419098.10933777", "2017-05-13");
                        students.add(student);
                        Collections.sort(students, new Comparator<Student>() {
                            public int compare(Student obj1, Student obj2) {
                                // ## Ascending order
                                return obj1.studentName.compareToIgnoreCase(obj2.studentName);
                                // return Integer.valueOf(obj1.empId).compareTo(obj2.empId); // To compare integer values

                                // ## Descending order
                                // return obj2.firstName.compareToIgnoreCase(obj1.firstName); // To compare string values
                                // return Integer.valueOf(obj2.empId).compareTo(obj1.empId); // To compare integer values
                            }
                        });
                        adapter.notifyItemInserted(students.indexOf(student));

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        new Instabug.Builder(getApplication(), AppConfig.INSTABUG_KEY)
                .setInvocationEvent(InstabugInvocationEvent.SHAKE)
                .build();
        Instabug.identifyUser(name, email);

        //getClassStudents(classID);
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
    protected void onResume() {
        super.onResume();

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(AppConfig.PUSH_NOTIFICATION));

        // clear the notification area when the app is opened
        NotificationUtils.clearNotifications(getApplicationContext());

        getClassStudents(classID);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    private void showStudent() {
        JSONObject jsonObject = null;
        students = new ArrayList<>();
        try {
            jsonObject = new JSONObject(JSON_STRING);
            JSONArray result = jsonObject.getJSONArray("students");

            for (int i = 0; i < result.length(); i++) {
                JSONObject jo = result.getJSONObject(i);
                String studentID = jo.getString("studentID");
                String studentName = jo.getString("studentName");
                String studentPresent = jo.getString("studentPresent");
                String studentPhoto = jo.getString("studentPhoto");
                String studentActive = jo.getString("studentActive");
                String studentInactiveDate = jo.getString("inactiveDate");

                if (!studentActive.equals("yes")) continue;

                Student student = new Student(studentID, studentName, studentPhoto, studentPresent, classID, accountID, currentDate);
                students.add(student);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        adapter = new StudentListAdapter(this, R.layout.student_list_item, students);
        listView.setAdapter(adapter);
    }

    /**
     * Function to set selected student deleted from class in MySQL DB
     */
    public void deleteStudentFromClass(final String studentID, final String classID, final Context context) {
        // Tag used to cancel the request
        String tag_string_req = "req_delete_student_from_class";


        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_DELETE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Delete Student Response: " + response);

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
                        Toast.makeText(context,
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
                Toast.makeText(context,
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("classID", classID);
                params.put("studentID", studentID);
                params.put("deleteType", "studentFromClass");
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);


    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     */
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
     */
    public void addStudentAttendance(final String studentID, final Boolean studentPresent, final String classID, final String accountID, final String currentDate, final Context context) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_attendance";

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_ADD_ATTENDANCE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Add Attendance Response: " + response);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        //Toast.makeText(getApplicationContext(), "Classes loaded!", Toast.LENGTH_LONG).show();
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(context,
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
                Toast.makeText(context,
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {

            String studentAttend = "";

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                if (studentPresent) {
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
     */
    private void getClassStudents(final String classID) {
        // Tag used to cancel the request
        String tag_string_req = "req_get_class_students";

        pDialog.setMessage("Loading Students...");

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_GET_CLASS_STUDENTS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                //Log.d(TAG, "Get Students Response: " + response.toString());

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

    private void showFABMenu() {
        isFABOpen = true;
        fabLayout1.setVisibility(View.VISIBLE);
        fabLayout2.setVisibility(View.VISIBLE);

        fabBGLayout.animate()
                .alpha(1.0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        fabBGLayout.setVisibility(View.VISIBLE);
                    }
                });

        fab.animate().rotationBy(180);
        fab.setImageResource(R.drawable.ic_close_black_24dp);
        fabLayout1.animate().translationY(-getResources().getDimension(R.dimen.standard_65));
        fabLayout2.animate().translationY(-getResources().getDimension(R.dimen.standard_110));
    }

    private void closeFABMenu() {
        isFABOpen = false;

        fabBGLayout.animate()
                .alpha(0.0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        fabBGLayout.setVisibility(View.GONE);
                    }
                });

        fab.animate().rotationBy(-180);
        fab.setImageResource(R.drawable.ic_add_black_24dp);
        fabLayout1.animate().translationY(0);
        fabLayout2.animate().translationY(0).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (!isFABOpen) {
                    fabLayout1.setVisibility(View.GONE);
                    fabLayout2.setVisibility(View.GONE);
                }

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isFABOpen) {
            closeFABMenu();
        } else {
            super.onBackPressed();
        }
    }

}
