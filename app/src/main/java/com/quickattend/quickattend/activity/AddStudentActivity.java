package com.quickattend.quickattend.activity;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.adapters.AddStudentAdapter;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;
import com.instabug.library.Instabug;
import com.instabug.library.invocation.InstabugInvocationEvent;
import com.soundcloud.android.crop.Crop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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

    private String studentGender ="";

    String currentDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String accountID;
    private String email;

    private boolean editMode = false;
    private boolean studentOnly = false;
    private boolean didSelectItem = false;

    List<String> studentList = new ArrayList<String>();

    private String classID;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private final int REQUEST_PERMISSION_STORAGE = 2;
    private final int REQUEST_CHECK_SETTINGS = 3;
    String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};
    final CharSequence[] dialogItems = {"Take Photo", "Choose Photo"};

    private Calendar studentDOBCalendar = Calendar.getInstance();
    private Calendar studentEnrollDateCalendar = Calendar.getInstance();
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

    DatePickerDialog.OnDateSetListener eDate = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            // TODO Auto-generated method stub
            studentEnrollDateCalendar.set(Calendar.YEAR, year);
            studentEnrollDateCalendar.set(Calendar.MONTH, monthOfYear);
            studentEnrollDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateStudentEnrollDate();
        }

    };

    String selectedDate;

    private Bitmap studentPhotoBitmap;
    private final int PICK_IMAGE_REQUEST = 4;
    private static int REQUEST_CROP_PICTURE = 5;
    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        email = user.get("email");
        String name = user.get("name");

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

        if (getIntent().hasExtra("editMode")) {
            editMode = true;
            inputStudentName.setText(getIntent().getStringExtra("studentName"));
            inputStudentPhone.setText(getIntent().getStringExtra("studentPhone"));
            inputStudentEmail.setText(getIntent().getStringExtra("studentEmail"));
            inputStudentAddress.setText(getIntent().getStringExtra("studentAddress"));
            inputStudentMedInfo.setText(getIntent().getStringExtra("studentMedInfo"));

            inputStudentID.setText(getIntent().getStringExtra("studentID"));

            studentGender = getIntent().getStringExtra("studentGender");
            if(studentGender == null){
                studentGender = "";
            }

            String myFormat = "yyyy-MM-dd"; //In which you need put here
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

            if (!getIntent().getStringExtra("studentDOB").equals("0000-00-00")) {
                try {
                    studentDOBCalendar.setTime(sdf.parse(getIntent().getStringExtra("studentDOB")));
                    updateStudentDOB();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                inputStudentDOB.setText("");
            }

            if (!getIntent().getStringExtra("studentEnrollDate").equals("0000-00-00")) {
                try {
                    studentEnrollDateCalendar.setTime(sdf.parse(getIntent().getStringExtra("studentEnrollDate")));
                    updateStudentEnrollDate();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                inputStudentEnrollDate.setText("");
            }

            ((RadioButton) findViewById(R.id.radioMale)).setChecked(getIntent().getStringExtra("studentGender").equals("Male"));
            ((RadioButton) findViewById(R.id.radioFemale)).setChecked(getIntent().getStringExtra("studentGender").equals("Female"));

        } else if(getIntent().hasExtra("studentOnly")){
            studentOnly = true;
            hideableInfo.setVisibility(View.GONE);
        } else {
            //currentDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());
        }

        studentPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ActivityCompat.checkSelfPermission(AddStudentActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(AddStudentActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        showExplanation("Storage Permission Required", "We need access to your storage to temporarily store student photos!", Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE);
                    } else {
                        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE);
                    }

                    Log.e("LOC ERROR", "no perms!");
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(AddStudentActivity.this);
                builder.setTitle("Change Photo") //
                        .setItems(dialogItems, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) { //"Take Photo
                                    Log.i("OPTION", "Take Photo!");
                                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                                        File photoFile = null;
                                        try{
                                            photoFile = createImageFile();
                                        } catch(IOException ex){

                                        }
                                        if (photoFile != null) {
                                            Uri photoURI = FileProvider.getUriForFile(AddStudentActivity.this,
                                                    "com.quickattend.quickattend.fileprovider",
                                                    photoFile);
                                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                                        }
                                    }
                                } else if (which == 1) {
                                    Log.i("OPTION", "Choose Photo!");
                                    File photoFile = null;
                                    try{
                                        photoFile = createImageFile();
                                    } catch(IOException ex){

                                    }
                                    if (photoFile != null) {
                                        showFileChooser();
                                    }
                                }
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // TODO
                                dialog.dismiss();
                            }
                        });
                builder.show();

            }
        });

        inputStudentName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                didSelectItem = false;
                if(!studentOnly){
                    hideableInfo.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        inputStudentDOB.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
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
                    year = studentDOBCalendar.get(Calendar.YEAR);
                    month = studentDOBCalendar.get(Calendar.MONTH);
                    day = studentDOBCalendar.get(Calendar.DAY_OF_MONTH);

                    new DatePickerDialog(AddStudentActivity.this, date, year, month, day).show();
                }
            }
        });

        inputStudentEnrollDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
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
                    year = studentEnrollDateCalendar.get(Calendar.YEAR);
                    month = studentEnrollDateCalendar.get(Calendar.MONTH);
                    day = studentEnrollDateCalendar.get(Calendar.DAY_OF_MONTH);

                    new DatePickerDialog(AddStudentActivity.this, eDate, year, month, day).show();
                }
            }
        });

        classID = getIntent().getStringExtra("classID");
        if(classID == null){
            classID = "";
        }

        getTypeaheadStudents();

        new Instabug.Builder(getApplication(), AppConfig.INSTABUG_KEY)
                .setInvocationEvent(InstabugInvocationEvent.SHAKE)
                .build();
        Instabug.identifyUser(name, email);

    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
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

    public String getStringImage(Bitmap bmp) {

        if (bmp == null) {
            return "";
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED);
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    private void updateStudentDOB() {

        String myFormat = "MM/dd/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        selectedDate = sdf.format(studentDOBCalendar.getTime());

        inputStudentDOB.setText(selectedDate);
    }

    private void updateStudentEnrollDate() {

        String myFormat = "MM/dd/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        selectedDate = sdf.format(studentEnrollDateCalendar.getTime());

        inputStudentEnrollDate.setText(selectedDate);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File newPhoto = new File(mCurrentPhotoPath);
            Uri photoUri = Uri.fromFile(newPhoto);

            Crop.of(photoUri, photoUri).asSquare().start(AddStudentActivity.this);
        }

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            File newPhoto = new File(mCurrentPhotoPath);
            Uri photoUri = Uri.fromFile(newPhoto);

            Crop.of(filePath, photoUri).asSquare().start(AddStudentActivity.this);
        }

        if ((requestCode == Crop.REQUEST_CROP) && (resultCode == RESULT_OK)) {
            // When we are done cropping, display it in the ImageView.
            setPic();
        }
    }

    /**
     * Function to get all of user's classes from MySQL DB
     */
    private void getTypeaheadStudents() {
        // Tag used to cancel the request
        String tag_string_req = "req_get_typeahead_students";

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_TYPEAHEAD_ACCOUNT_STUDENTS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Get Students Response: " + response.toString());
                ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        JSON_STRING = response;
                        JSONArray result = jObj.getJSONArray("students");

                        for (int i = 0; i < result.length(); i++) {
                            JSONObject jo = result.getJSONObject(i);
                            String studentID = jo.getString("studentID");
                            String studentName = jo.getString("studentName");

                            HashMap<String, String> student = new HashMap<>();
                            student.put("studentID", studentID);
                            student.put("studentName", studentName);
                            list.add(student);
                        }

                        AddStudentAdapter adapter = new AddStudentAdapter(
                                AddStudentActivity.this, list,
                                new String[]{"studentID", "studentName"},
                                new int[]{R.id.studentID, R.id.studentName});

                        inputStudentName.setAdapter(adapter);
                        inputStudentName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                HashMap<String, String> student = (HashMap<String, String>) parent.getItemAtPosition(position);

                                Log.i("FROMARRAY", student.get("studentName"));
                                inputStudentID.setText(student.get("studentID"));
                                inputStudentName.setText(student.get("studentName"));

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
        if (editMode) {
            getMenuInflater().inflate(R.menu.menu_update_student, menu);
        } else {
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
        String studentEmail = inputStudentEmail.getText().toString().trim();
        String studentAddress = inputStudentAddress.getText().toString().trim();
        String studentEnrollDate = inputStudentEnrollDate.getText().toString().trim();
        String studentMedInfo = inputStudentMedInfo.getText().toString().trim();

        switch (item.getItemId()) {
            case R.id.action_add_student:

                if (!studentName.isEmpty()) {
                    if(studentOnly){
                        if(didSelectItem){
                            addExistingStudent(studentID);
                        }
                        Toast.makeText(getApplicationContext(),
                                "Please select a student!", Toast.LENGTH_LONG)
                                .show();
                        return false;
                    } else {
                        addStudent(studentName, studentBirthday, studentPhone, studentEmail, studentAddress, studentEnrollDate, studentMedInfo, studentID);
                    }

                    return true;
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Student name is required!", Toast.LENGTH_LONG)
                            .show();
                    return false;
                }

            case R.id.action_update_student:

                if (!studentName.isEmpty()) {
                    updateStudent(studentName, studentBirthday, studentPhone, studentEmail, studentAddress, studentEnrollDate, studentMedInfo, studentID);

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
     */
    private void addExistingStudent(final String studentID) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_existing_student";

        pDialog.setMessage("Enrolling Student...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_ADD_EXISTING_STUDENT, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Add Student Response: " + response);
                hideDialog();

                inputStudentName.setText("");
                inputStudentDOB.setText("");
                inputStudentID.setText("");
                inputStudentPhone.setText("");
                inputStudentEmail.setText("");
                inputStudentAddress.setText("");
                inputStudentEnrollDate.setText("");
                inputStudentMedInfo.setText("");

                ((RadioButton) findViewById(R.id.radioMale)).setChecked(false);
                ((RadioButton) findViewById(R.id.radioFemale)).setChecked(false);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        Toast.makeText(getApplicationContext(), "Student enrolled!", Toast.LENGTH_LONG).show();
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

                //Converting Bitmap to String
                String image = getStringImage(studentPhotoBitmap);

                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("classID", classID);
                params.put("studentID", studentID);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Function to store student in MySQL database will post params(class, name,
     * age, existing id) to add student url
     */
    private void addStudent(final String studentName, final String studentDOB, final String studentPhone, final String studentEmail, final String studentAddress, final String studentEnrollDate, final String studentMedInfo, final String studentID) {
        // Tag used to cancel the request
        String tag_string_req = "req_add_student";

        pDialog.setMessage("Adding Student...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_ADD_STUDENT, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Add Student Response: " + response);
                hideDialog();

                inputStudentName.setText("");
                inputStudentDOB.setText("");
                inputStudentID.setText("");
                inputStudentPhone.setText("");
                inputStudentEmail.setText("");
                inputStudentAddress.setText("");
                inputStudentEnrollDate.setText("");
                inputStudentMedInfo.setText("");

                ((RadioButton) findViewById(R.id.radioMale)).setChecked(false);
                ((RadioButton) findViewById(R.id.radioFemale)).setChecked(false);

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

                //Converting Bitmap to String
                String image = getStringImage(studentPhotoBitmap);

                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("classID", classID);
                params.put("accountID", accountID);
                params.put("studentName", studentName);
                params.put("studentDOB", studentDOB);
                params.put("studentPhone", studentPhone);
                params.put("studentGender", studentGender);
                params.put("studentPhoto", image);
                params.put("studentEmail", studentEmail);
                params.put("studentAddress", studentAddress);
                params.put("studentEnrollDate", studentEnrollDate);
                params.put("studentMedInfo", studentMedInfo);
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
     */
    private void updateStudent(final String studentName, final String studentDOB, final String studentPhone, final String studentEmail, final String studentAddress, final String studentEnrollDate, final String studentMedInfo, final String studentID) {
        // Tag used to cancel the request
        String tag_string_req = "req_update_student";

        pDialog.setMessage("Updating Student...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_UPDATE_STUDENT, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Update Student Response: " + response);
                hideDialog();

                inputStudentName.setText("");
                inputStudentDOB.setText("");
                inputStudentID.setText("");
                inputStudentPhone.setText("");
                inputStudentEmail.setText("");
                inputStudentAddress.setText("");
                inputStudentEnrollDate.setText("");
                inputStudentMedInfo.setText("");

                ((RadioButton) findViewById(R.id.radioMale)).setChecked(false);
                ((RadioButton) findViewById(R.id.radioFemale)).setChecked(false);

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

                //Converting Bitmap to String
                String image = getStringImage(studentPhotoBitmap);

                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountID", accountID);
                params.put("studentID", studentID);
                params.put("studentName", studentName);
                params.put("studentDOB", studentDOB);
                params.put("studentPhone", studentPhone);
                params.put("studentGender", studentGender);
                params.put("studentPhoto", image);
                params.put("studentEmail", studentEmail);
                params.put("studentAddress", studentAddress);
                params.put("studentEnrollDate", studentEnrollDate);
                params.put("studentMedInfo", studentMedInfo);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = studentPicture.getWidth();
        int targetH = studentPicture.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        studentPhotoBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        studentPicture.setImageBitmap(studentPhotoBitmap);
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
