package com.quickattend.quickattend.activity;

import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.adapters.StudentFullListAdapter;
import com.quickattend.quickattend.adapters.StudentListAdapter;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.models.Student;
import com.quickattend.quickattend.models.StudentFull;
import com.quickattend.quickattend.utils.RecyclerViewEmptySupport;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;

public class StudentListFragment extends Fragment {
    private static final String TAG = StudentListFragment.class.getSimpleName();
    private ProgressDialog pDialog;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String accountID;
    private String name;
    private String email;

    private RecyclerViewEmptySupport listView;
    private View view;
    private List<StudentFull> students;
    private StudentFullListAdapter adapter;
    private ImageButton addStudentImage;

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

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("All Students");

        // SqLite database handler
        db = new SQLiteHandler(getActivity());

        // session manager
        session = new SessionManager(getActivity());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        // Fetching user details from sqlite
        final HashMap<String, String> user = db.getUserDetails();

        accountID = user.get("account_id");
        name = user.get("name");
        email = user.get("email");

        view = inflater.inflate(R.layout.fragment_student_list, container, false);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());

        listView = (RecyclerViewEmptySupport) view.findViewById(R.id.listView);
        listView.setHasFixedSize(true);
        listView.setLayoutManager(layoutManager);

        addStudentImage = (ImageButton) view.findViewById(R.id.imageAddStudent);

        addStudentImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddStudentActivity.class);
                startActivity(intent);
            }
        });

        /*
         * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
         * performs a swipe-to-refresh gesture.
         */
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.i(TAG, "onRefresh called from SwipeRefreshLayout");

                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        getAllStudents();
                    }
                }
        );
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark);

        return view;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_student_list, menu);
        super.onCreateOptionsMenu(menu,inflater);

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        EditText searchEditText = (EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        View searchLayout = searchView.findViewById((android.support.v7.appcompat.R.id.search_plate));
        searchEditText.setHint("Search students");
        searchEditText.setTextColor(getResources().getColor(R.color.white));
        searchEditText.setHintTextColor(getResources().getColor(R.color.white));
        searchLayout.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.MULTIPLY);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if(query.isEmpty()){
                    query = "backspaceNOW";
                }

                Log.e("QUERY", query + " searched");
                adapter.getFilter().filter(query);
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_export_all_students) {
            exportStudents();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openSingleStudent(final String studentID, Context context){
        Intent i = new Intent(context, SingleStudentActivity.class);
        i.putExtra("studentID", studentID);
        context.startActivity(i);
    }

    @Override
    public void onResume() {
        super.onResume();
        getAllStudents();
    }

    private void showStudent(){
        JSONObject jsonObject = null;
        students = new ArrayList<>();
        try {
            jsonObject = new JSONObject(JSON_STRING);
            JSONArray result = jsonObject.getJSONArray("students");

            for(int i = 0; i<result.length(); i++){
                JSONObject jo = result.getJSONObject(i);
                String studentID = jo.getString("studentID");
                String studentName = jo.getString("studentName");
                String studentPhone = jo.getString("studentPhone");
                String studentPhoto = jo.getString("studentPhoto");

                StudentFull student = new StudentFull(studentID, studentName, studentPhoto, accountID);
                students.add(student);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        adapter = new StudentFullListAdapter(getActivity(), R.layout.student_list_full_item, students);
        listView.setAdapter(adapter);
        listView.setEmptyView(view.findViewById(R.id.empty_list_item));
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

    /**
     * Export students from DB to user email
     * */
    private void exportStudents() {
        // Tag used to cancel the request
        String tag_string_req = "req_export_all_students";

        pDialog.setMessage("Exporting Students...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_EXPORT_ALL_STUDENTS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Export Students Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        hideDialog();
                        Toast.makeText(getActivity(), "Students exported. Please check your email", Toast.LENGTH_LONG).show();
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
                params.put("accountID", accountID);
                params.put("userEmail", email);
                params.put("userName", name);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }


    /**
     * Function to get all of account's students from MySQL DB
     * */
    private void getAllStudents() {
        // Tag used to cancel the request
        String tag_string_req = "req_get_all_students";

        pDialog.setMessage("Loading Students...");

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_GET_ALL_STUDENTS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Get All Students Response: " + response);

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

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }
}
