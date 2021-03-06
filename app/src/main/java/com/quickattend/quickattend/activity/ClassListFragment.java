package com.quickattend.quickattend.activity;

import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.app.ProgressDialog;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

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

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;

public class ClassListFragment extends android.support.v4.app.ListFragment implements ListView.OnItemClickListener {
    private static final String TAG = ClassListFragment.class.getSimpleName();
    private ProgressDialog pDialog;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String userID;
    private String accountID;

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

    String currentDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());

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

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("All Classes");

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

        View view = inflater.inflate(R.layout.fragment_class_list, container, false);
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        getAllClasses(false);

        /*
         * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
         * performs a swipe-to-refresh gesture.
         */
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        getAllClasses(false);
                    }
                }
        );
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark);

        return view;

    }

    private void updateStudentDate() {

        String myFormat = "MM/dd/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        currentDate = sdf.format(studentCalendar.getTime());

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("My Classes: " + currentDate);
        getTodayClasses();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_class_list, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_change_date) {
            new DatePickerDialog(getActivity(), date, studentCalendar
                    .get(Calendar.YEAR), studentCalendar.get(Calendar.MONTH),
                    studentCalendar.get(Calendar.DAY_OF_MONTH)).show();

            return true;
        } else if (id == R.id.action_view_all_classes) {
            getAllClasses(true);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        HashMap<String,String> map =(HashMap) getListView().getItemAtPosition(position);
        String classID = map.get("classID").toString();
        String className = map.get("className").toString();

        Intent i = new Intent(getActivity(), SingleClassActivity.class);

        i.putExtra("classID",classID);
        i.putExtra("className", className);
        i.putExtra("classDate", currentDate);
        getActivity().startActivity(i);

        //Intent intent = new Intent(getActivity(), SingleClassActivity.class);
        //intent.putExtra("classID",classID);
        //startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        getAllClasses(false);
    }

    private void showClass(){
        JSONObject jsonObject = null;
        ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String, String>>();
        try {
            jsonObject = new JSONObject(JSON_STRING);
            JSONArray result = jsonObject.getJSONArray("classes");

            for(int i = 0; i<result.length(); i++){
                JSONObject jo = result.getJSONObject(i);
                String classID = jo.getString("classID");
                String className = jo.getString("className");
                String classLocation = jo.getString("classLocation");
                String classTime = jo.getString("classTime");
                String classCount = jo.getString("classCount");

                HashMap<String,String> classes = new HashMap<>();
                classes.put("classID",classID);
                classes.put("className",className);
                classes.put("classTime", classTime);
                classes.put("classLocation",classLocation);
                classes.put("classCount", classCount);
                list.add(classes);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        ListAdapter adapter = new SimpleAdapter(
                getActivity(), list, R.layout.list_item,
                new String[]{"classID", "className", "classTime", "classLocation", "classCount"},
                new int[]{R.id.classID, R.id.className, R.id.classTime, R.id.classLocation, R.id.classCount});

        setListAdapter(adapter);
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     * */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Intent intent = new Intent(this, ViewEmployee.class);
        HashMap<String,String> map =(HashMap)parent.getItemAtPosition(position);
        String className = map.get("className").toString();
        //intent.putExtra("classID",empId);
        //startActivity(intent);
    }

    /**
     * Function to get all of user's classes from MySQL DB
     * */
    private void getAllClasses(final boolean getAccountClasses) {
        // Tag used to cancel the request
        String tag_string_req = "req_get_all_classes";

        pDialog.setMessage("Loading Classes...");

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_GET_ALL_CLASSES, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                //Log.d(TAG, "Get Class Response: " + response.toString());

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        JSON_STRING = response;
                        showClass();
                        //Toast.makeText(getApplicationContext(), "Classes loaded!", Toast.LENGTH_LONG).show();
                    } else {

                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getActivity(),
                                errorMsg, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSwipeRefreshLayout.setRefreshing(false);

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Fetching Error: " + error.getMessage());
                Toast.makeText(getActivity(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("userID", userID);

                if(getAccountClasses){
                    params.put("accountID", accountID);
                }

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Function to get all of user's classes from MySQL DB
     * */
    private void getTodayClasses() {
        // Tag used to cancel the request
        String tag_string_req = "req_get_today_classes";

        pDialog.setMessage("Loading Classes...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_GET_TODAY_CLASSES, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                //Log.d(TAG, "Get Class Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        JSON_STRING = response;
                        showClass();
                        //Toast.makeText(getApplicationContext(), "Classes loaded!", Toast.LENGTH_LONG).show();
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
                mSwipeRefreshLayout.setRefreshing(false);

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Fetching Error: " + error.getMessage());
                Toast.makeText(getActivity(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("userID", userID);
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
