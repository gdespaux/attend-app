package com.quickattend.quickattend.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LeadFragment extends Fragment {
    private static final String TAG = LeadFragment.class.getSimpleName();
    private ProgressDialog pDialog;
    private EditText inputStudentName;
    private EditText inputStudentDOB;
    private EditText inputStudentPhone;
    private EditText inputStudentEmail;
    private EditText inputStudentAddress;
    private EditText inputStudentMiscInfo;

    private RadioGroup radioGroup;
    private Button submitButton;

    private SQLiteHandler db;
    private SessionManager session;
    private String userID;
    private String accountID;
    Activity mActivity;
    View view;

    private Calendar studentDOBCalendar = Calendar.getInstance();
    DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            // TODO Auto-generated method stub
            studentDOBCalendar.set(Calendar.YEAR, year);
            studentDOBCalendar.set(Calendar.MONTH, monthOfYear);
            studentDOBCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateStudentDOB();
        }

    };

    String selectedDate;
    private String studentGender = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Progress dialog
        pDialog = new ProgressDialog(getActivity());
        pDialog.setCancelable(false);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Sign up");

        // SqLite database handler
        db = new SQLiteHandler(getActivity());

        // session manager
        session = new SessionManager(getActivity());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        // Fetching user details from sqlite
        final HashMap<String, String> user = db.getUserDetails();

        userID = user.get("uid");
        accountID = user.get("account_id");
        String name = user.get("name");
        String email = user.get("email");

        view = inflater.inflate(R.layout.fragment_lead, container, false);

        inputStudentDOB = (EditText) view.findViewById(R.id.studentDOB);
        inputStudentPhone = (EditText) view.findViewById(R.id.studentPhone);
        inputStudentName = (EditText) view.findViewById(R.id.studentName);

        inputStudentEmail = (EditText) view.findViewById(R.id.studentEmail);
        inputStudentAddress = (EditText) view.findViewById(R.id.studentAddress);
        inputStudentMiscInfo = (EditText) view.findViewById(R.id.studentMiscInfo);

        radioGroup = (RadioGroup) view.findViewById(R.id.genderRadios);

        submitButton = (Button) view.findViewById(R.id.submitButton);

        inputStudentPhone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        inputStudentDOB.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    int year;
                    int month;
                    int day;

                    year = studentDOBCalendar.get(Calendar.YEAR);
                    month = studentDOBCalendar.get(Calendar.MONTH);
                    day = studentDOBCalendar.get(Calendar.DAY_OF_MONTH);

                    new DatePickerDialog(getActivity(), date, year, month, day).show();
                }
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                // Check which radio button was clicked
                switch (checkedId) {
                    case R.id.radioMale:
                        studentGender = "Male";
                        break;
                    case R.id.radioFemale:
                        studentGender = "Female";
                        break;
                }
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String studentName = inputStudentName.getText().toString().trim();
                String studentBirthday = inputStudentDOB.getText().toString().trim();
                String studentPhone = inputStudentPhone.getText().toString().trim();
                String studentEmail = inputStudentEmail.getText().toString().trim();
                String studentAddress = inputStudentAddress.getText().toString().trim();
                String studentMedInfo = inputStudentMiscInfo.getText().toString().trim();

                if (!studentName.isEmpty()) {
                        addLead(studentName, studentBirthday, studentPhone, studentEmail, studentAddress, studentMedInfo);
                } else {
                    Toast.makeText(getActivity(),
                            "Student name is required!", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        return view;

    }

    private void updateStudentDOB() {

        String myFormat = "MM/dd/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        selectedDate = sdf.format(studentDOBCalendar.getTime());

        inputStudentDOB.setText(selectedDate);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();
    }

    /**
     * Function to store student in MySQL database will post params(class, name,
     * age, existing id) to add lead url
     */
    private void addLead(final String studentName, final String studentDOB, final String studentPhone, final String studentEmail, final String studentAddress, final String studentMiscInfo) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_lead";

        pDialog.setMessage("Sending...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_ADD_LEAD, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Add Lead Response: " + response);
                hideDialog();

                inputStudentName.setText("");
                inputStudentDOB.setText("");
                inputStudentPhone.setText("");
                inputStudentEmail.setText("");
                inputStudentAddress.setText("");
                inputStudentMiscInfo.setText("");

                ((RadioButton) view.findViewById(R.id.radioMale)).setChecked(false);
                ((RadioButton) view.findViewById(R.id.radioFemale)).setChecked(false);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        Toast.makeText(getActivity(), "Info sent. Thank you!", Toast.LENGTH_LONG).show();
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getActivity(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Submission Error: " + error.getMessage());
                Toast.makeText(getActivity(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {


                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountID", accountID);
                params.put("studentName", studentName);
                params.put("studentDOB", studentDOB);
                params.put("studentPhone", studentPhone);
                params.put("studentGender", studentGender);
                params.put("studentEmail", studentEmail);
                params.put("studentAddress", studentAddress);
                params.put("studentMiscInfo", studentMiscInfo);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     * */
    private void logoutUser() {
        session.setLogin(false);
        session.enterLeadMode(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
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
