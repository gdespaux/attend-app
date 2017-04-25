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
import android.widget.LinearLayout;
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
    private EditText inputSecondEmail;
    private EditText inputPassword;
    private EditText inputCode;

    private LinearLayout passwordResetForm;
    private LinearLayout enterCodeForm;

    private ProgressDialog pDialog;
    private SessionManager session;
    private SQLiteHandler db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        inputEmail = (EditText) findViewById(R.id.email);
        inputSecondEmail = (EditText) findViewById(R.id.secondEmail);
        inputPassword = (EditText) findViewById(R.id.password);
        inputCode = (EditText) findViewById(R.id.code);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);
        btnHaveCode = (Button) findViewById(R.id.btnHaveCode);
        btnEnterCode = (Button) findViewById(R.id.btnEnterCode);

        passwordResetForm = (LinearLayout) findViewById(R.id.passwordResetForm);
        enterCodeForm = (LinearLayout) findViewById(R.id.enterCodeForm);

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
                    showCodeScreen();
                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),
                            "Please enter your email!", Toast.LENGTH_LONG)
                            .show();
                }
            }

        });

        // Reset button Click Event
        btnHaveCode.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                showCodeScreen();
            }

        });

        // Reset button Click Event
        btnEnterCode.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                String email = inputSecondEmail.getText().toString().trim();
                String code = inputCode.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                // Check for empty data in the form
                if (!email.isEmpty() && !code.isEmpty() && !password.isEmpty()) {
                    // send reset email
                    resetPassword(email, code, password);
                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),
                            "Please enter all fields!", Toast.LENGTH_LONG)
                            .show();
                }
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

    public void showCodeScreen(){

        passwordResetForm.setVisibility(View.GONE);
        enterCodeForm.setVisibility(View.VISIBLE);

    }

    /**
     * function to send reset email and store token in mysql db
     * */
    private void sendResetEmail(final String email) {
        // Tag used to cancel the request
        String tag_string_req = "req_send_reset_email";

        pDialog.setMessage("Generating Code ...");

        inputEmail.setText("");

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
                        Toast.makeText(getApplicationContext(), "Code Sent! Please check your email", Toast.LENGTH_LONG).show();
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
                params.put("userEmail", email);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * function to send reset email and store token in mysql db
     * */
    private void resetPassword(final String email, final String resetCode, final String password) {
        // Tag used to cancel the request
        String tag_string_req = "req_reset_password";

        pDialog.setMessage("Resetting Password ...");

        inputPassword.setText("");

        showDialog();

        StringRequest strReq = new StringRequest(Method.POST,
                AppConfig.URL_RESET_PASSWORD, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Reset Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        Toast.makeText(getApplicationContext(), "Password reset! Please login", Toast.LENGTH_LONG).show();
                        // Launch login activity
                        Intent intent = new Intent(
                                ResetPasswordActivity.this,
                                LoginActivity.class);
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
                params.put("userEmail", email);
                params.put("userCode", resetCode);
                params.put("userPassword", password);

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