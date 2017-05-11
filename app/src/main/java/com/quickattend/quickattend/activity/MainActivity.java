package com.quickattend.quickattend.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
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
import com.quickattend.quickattend.utils.NotificationUtils;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;
import com.google.firebase.messaging.FirebaseMessaging;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Fragment fragment = null;

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView txtName;
    private TextView txtEmail;
    private CircularNetworkImageView userPhotoView;

    private DrawerLayout drawer;

    private SQLiteHandler db;
    private SessionManager session;

    private String fabAction;
    private String userID;
    private String userPhoto;

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    FloatingActionButton fab;
    private NavigationView navigationView;
    ImageLoader imageLoader = AppController.getInstance().getImageLoader();

    private boolean leadMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        if(session.inLeadMode()){
            leadMode = true;
        }

        // Fetching user details from sqlite
        final HashMap<String, String> user = db.getUserDetails();

        String name = user.get("name");
        String email = user.get("email");
        userPhoto = user.get("userPhoto");

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launching the add class activity
                Intent intent;
                if (fabAction.equals("Class")) {
                    intent = new Intent(MainActivity.this, AddClassActivity.class);
                } else if (fabAction.equals("Student")) {
                    intent = new Intent(MainActivity.this, AddStudentActivity.class);
                } else {
                    intent = new Intent(MainActivity.this, AddClassActivity.class);
                }
                startActivity(intent);
            }
        });

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);

        txtName = (TextView) headerView.findViewById(R.id.name);
        txtEmail = (TextView) headerView.findViewById(R.id.email);
        userPhotoView = (CircularNetworkImageView) headerView.findViewById(R.id.userPhoto);
        // Displaying the user details on the screen
        txtName.setText(name);
        txtEmail.setText(email);
        userPhotoView.setImageUrl(userPhoto, imageLoader);

        //default fragment loaded
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.frame_container, new HomeFragment()).commit();
        fabAction = "Class";

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // checking for type intent filter
                if (intent.getAction().equals(AppConfig.REGISTRATION_COMPLETE)) {
                    // gcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    FirebaseMessaging.getInstance().subscribeToTopic(AppConfig.TOPIC_GLOBAL);

                    displayFirebaseRegId();

                } else if (intent.getAction().equals(AppConfig.PUSH_NOTIFICATION)) {
                    // new push notification is received

                    String message = intent.getStringExtra("message");

                    Toast.makeText(getApplicationContext(), "Push notification: " + message, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Got ya message!");
                }

                Log.d(TAG, "Uh oh!");
            }
        };

        displayFirebaseRegId();

        new Instabug.Builder(getApplication(), AppConfig.INSTABUG_KEY)
                .setInvocationEvent(InstabugInvocationEvent.NONE)
                .build();
        Instabug.identifyUser(name, email);

        Log.e("LEADMODE", "Is on: " + leadMode);
    }

    // Fetches reg id from shared preferences
    // and displays on the screen
    private void displayFirebaseRegId() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(AppConfig.SHARED_PREF, 0);
        String regId = pref.getString("regId", null);

        Log.e(TAG, "Firebase reg id: " + regId);

        if (!TextUtils.isEmpty(regId)) {
            //Toast.makeText(getApplicationContext(), "Firebase Reg Id: " + regId, Toast.LENGTH_LONG).show();
            sendRegistrationToServer(regId);
        } else {
            Toast.makeText(getApplicationContext(), "Firebase Reg Id is not received yet!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register GCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(AppConfig.REGISTRATION_COMPLETE));

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(AppConfig.PUSH_NOTIFICATION));

        // clear the notification area when the app is opened
        NotificationUtils.clearNotifications(getApplicationContext());

        Log.e("LEADMODE", "Is on: " + leadMode);

        if(leadMode){
            fragment = new LeadFragment();
            fab.hide();
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.activity_main_drawer_empty);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentManager.beginTransaction()
                    .add(R.id.frame_container, fragment).commit();
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            fragment = new HomeFragment();
            fabAction = "Class";
            fab.show();
        } else if (id == R.id.nav_class_list) {
            fragment = new ClassListFragment();
            fabAction = "Class";
            fab.show();
        } else if (id == R.id.nav_student_list) {
            fragment = new StudentListFragment();
            fabAction = "Student";
            fab.show();
        } else if (id == R.id.nav_reports) {
            fragment = new ReportsFragment();
            fab.hide();
        } else if (id == R.id.nav_profile) {
            fragment = new UserProfileFragment();
            fab.hide();
        } else if (id == R.id.nav_leads) {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Lead Mode")
                    .setMessage("This will put the device into public Lead Mode. Are you sure?\n(You can only exit Lead Mode by logging out and then back in via the left menu)")
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            session.enterLeadMode(true);
                            Log.e("LEADMODE", "Is on: " + leadMode);
                            fragment = new LeadFragment();
                            fab.hide();
                            navigationView.getMenu().clear();
                            navigationView.inflateMenu(R.menu.activity_main_drawer_empty);
                            FragmentManager fragmentManager = getSupportFragmentManager();
                            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                            fragmentManager.beginTransaction()
                                    .replace(R.id.frame_container, fragment).commit();
                        }
                    });
            builder.show();
        } else if (id == R.id.nav_feedback) {
            drawer.closeDrawer(GravityCompat.START, false);
            Instabug.invoke();
        } else if (id == R.id.nav_about) {
            fragment = new AboutFragment();
            fab.hide();
        } else if (id == R.id.nav_log_out) {
            session.enterLeadMode(false);
            logoutUser();
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.frame_container, fragment).addToBackStack(null).commit();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Function to store device id in MySQL database will post params(token)
     * to register url
     */
    public void sendRegistrationToServer(final String token) {

        SharedPreferences pref = getApplicationContext().getSharedPreferences(AppConfig.SHARED_PREF, 0);

        if (token == "") {
            pref.getString("regID", "");
        }

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());

        // Fetching user details from sqlite
        final HashMap<String, String> user = db.getUserDetails();
        userID = user.get("uid");

        if (userID != null) {
            // Tag used to cancel the request
            String tag_string_req = "req_reg_id";

            // sending gcm token to server
            Log.e(TAG, "sendRegistrationToServer: " + token);

            StringRequest strReq = new StringRequest(Request.Method.POST,
                    AppConfig.URL_REG_ID, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Add Reg ID Response: " + response.toString());
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Device ID Reg Error: " + error.getMessage());
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    // Posting params to register url
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("userID", userID);
                    params.put("regID", token);

                    Log.e(TAG, "USERID: " + userID);
                    Log.e(TAG, "REGID: " + token);

                    return params;
                }

            };

            // Adding request to request queue
            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
        }
    }
}
