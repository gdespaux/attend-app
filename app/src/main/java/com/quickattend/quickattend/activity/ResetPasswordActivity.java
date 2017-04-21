package com.quickattend.quickattend.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;

public class ResetPasswordActivity extends Activity {
    private static final String TAG = ResetPasswordActivity.class.getSimpleName();

    private Button btnResetPassword;
    private Button btnHaveCode;
    private Button btnEnterCode;
    private EditText inputEmail;
    private EditText inputCode;

    private ProgressDialog pDialog;
    private SessionManager session;
    private SQLiteHandler db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputEmail = (EditText) findViewById(R.id.email);
        inputCode = (EditText) findViewById(R.id.code);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);
        btnHaveCode = (Button) findViewById(R.id.btnHaveCode);
        btnEnterCode = (Button) findViewById(R.id.btnEnterCode);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // Session manager
        session = new SessionManager(getApplicationContext());

        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            // User is already logged in. Take him to main activity
            Intent intent = new Intent(ResetPasswordActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        // Reset button Click Event
        btnResetPassword.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                String email = inputEmail.getText().toString().trim();

                // Check for empty data in the form
                if (!email.isEmpty()) {
                    // send reset email
                    sendResetEmail(email);
                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),
                            "Please enter your email!", Toast.LENGTH_LONG)
                            .show();
                }
            }

        });

        // Link to Password Reset Screen
        btnResetPassword.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),
                        ResetPasswordActivity.class);
                startActivity(i);
            }
        });

    }

    public void onSwitchButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((Switch) view).isChecked();

        if(checked){
            ((Switch) view).getThumbDrawable().setColorFilter(Color.argb(255, 29, 233, 182), PorterDuff.Mode.MULTIPLY);
            ((Switch) view).getTrackDrawable().setColorFilter(Color.argb(255, 178, 223, 219), PorterDuff.Mode.MULTIPLY);
        } else {
            ((Switch) view).getThumbDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            ((Switch) view).getTrackDrawable().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
        }
    }

    /**
     * function to send reset email and store token in mysql db
     * */
    private void sendResetEmail(final String email) {
        // Tag used to cancel the request
        String tag_string_req = "req_send_reset_email";

        pDialog.setMessage("Generating Code ...");
        showDialog();

        StringRequest strReq = new StringRequest(Method.POST,
                AppConfig.URL_SEND_RESET_CODE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Reset Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {

                        // Now store the user in SQLite
                        String uid = jObj.getString("uid");
                        String accountID = jObj.getString("account_id");

                        JSONObject user = jObj.getJSONObject("user");
                        String name = user.getString("name");

                        // Launch main activity
                        Intent intent = new Intent(ResetPasswordActivity.this,
                                MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Reset Code Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);

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