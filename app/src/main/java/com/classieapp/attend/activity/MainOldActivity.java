package com.classieapp.attend.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.classieapp.attend.R;
import com.classieapp.attend.utils.SQLiteHandler;
import com.classieapp.attend.utils.SessionManager;

import java.util.HashMap;

public class MainOldActivity extends AppCompatActivity {
    private static final String TAG = MainOldActivity.class.getSimpleName();
    private TextView txtName;
    private TextView txtEmail;
    private TextView txtAccountType;
    private Button btnLogout;
    private Button btnViewClasses;
    private ProgressDialog pDialog;

    private SQLiteHandler db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_old);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        txtName = (TextView) findViewById(R.id.name);
        txtEmail = (TextView) findViewById(R.id.email);
        txtAccountType = (TextView) findViewById(R.id.accountType);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnViewClasses = (Button) findViewById(R.id.viewClassesBtn);

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        // Fetching user details from sqlite
        final HashMap<String, String> user = db.getUserDetails();

        String name = user.get("name");
        String email = user.get("email");
        String accountType = user.get("accountType");

        // Displaying the user details on the screen
        txtName.setText(name);
        txtEmail.setText(email);
        txtAccountType.setText(accountType);

        // Logout button click event
        btnLogout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        // View classes click event
        btnViewClasses.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Launching the class list activity
                Intent intent = new Intent(MainOldActivity.this, ClassListFragment.class);
                startActivity(intent);
                //finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     * */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(MainOldActivity.this, LoginActivity.class);
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
