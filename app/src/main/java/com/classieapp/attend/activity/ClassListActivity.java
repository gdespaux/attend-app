package com.classieapp.attend.activity;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.ChangeBounds;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.classieapp.attend.R;
import com.classieapp.attend.app.AppConfig;
import com.classieapp.attend.app.AppController;
import com.classieapp.attend.utils.SQLiteHandler;
import com.classieapp.attend.utils.SessionManager;

public class ClassListActivity extends android.support.v4.app.ListFragment implements ListView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = ClassListActivity.class.getSimpleName();
    private ProgressDialog pDialog;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String userID;

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // set an exit transition
        getActivity().getWindow().setSharedElementEnterTransition(new ChangeBounds());
        getActivity().getWindow().setSharedElementExitTransition(new ChangeBounds());
        getActivity().getWindow().setEnterTransition(new Fade());
        getActivity().getWindow().setExitTransition(new Explode());
        getActivity().getWindow().setAllowEnterTransitionOverlap(true);

        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Progress dialog
        pDialog = new ProgressDialog(getActivity());
        pDialog.setCancelable(false);

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
        String name = user.get("name");
        String email = user.get("email");

        View view = inflater.inflate(R.layout.fragment_class_list, container, false);
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        getAllClasses(userID);

        /*
         * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
         * performs a swipe-to-refresh gesture.
         */
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        return view;

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Log.i("Clicked!", "Item was clicked!");
        HashMap<String,String> map =(HashMap) getListView().getItemAtPosition(position);
        String classID = map.get("classID").toString();

        Intent i = new Intent(getActivity(), SingleClassActivity.class);

        View sharedView = v;
        String transitionName = "transClassName";

        //ActivityOptions transitionActivityOptions = ActivityOptions.makeSceneTransitionAnimation(getActivity(),
        //        Pair.create(v, "transClassName"),
        //        Pair.create(v, "transClassTime"),
        //        Pair.create(v, "transClassLocation"));

        ActivityOptions transitionActivityOptions = ActivityOptions.makeSceneTransitionAnimation(getActivity(), sharedView, transitionName);
        i.putExtra("classID",classID);
        getActivity().startActivity(i, transitionActivityOptions.toBundle());

        //Intent intent = new Intent(getActivity(), SingleClassActivity.class);
        //intent.putExtra("classID",classID);
        //startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        getAllClasses(userID);
    }

    @Override
    public void onRefresh() {
        Log.i(TAG, "onRefresh called from SwipeRefreshLayout");

        // This method performs the actual data-refresh operation.
        // The method calls setRefreshing(false) when it's finished.
        getAllClasses(userID);
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
                new String[]{"classID", "className", "classTime", "classLocation"},
                new int[]{R.id.classID, R.id.className, R.id.classTime, R.id.classLocation});

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
    private void getAllClasses(final String userID) {
        // Tag used to cancel the request
        String tag_string_req = "req_get_all_classes";

        pDialog.setMessage("Loading Classes...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_GET_ALL_CLASSES, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Get Class Response: " + response.toString());
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
