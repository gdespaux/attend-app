package com.quickattend.quickattend.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.content.Intent;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.quickattend.quickattend.R;
import com.quickattend.quickattend.app.AppConfig;
import com.quickattend.quickattend.app.AppController;
import com.quickattend.quickattend.utils.CircularNetworkImageView;
import com.quickattend.quickattend.utils.SQLiteHandler;
import com.quickattend.quickattend.utils.SessionManager;

public class UserProfileFragment extends Fragment {
    private static final String TAG = UserProfileFragment.class.getSimpleName();

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String userID;
    private String accountID;

    private TextView userNameText;
    private TextView userEmailText;
    private TextView userAccountTypeText;
    private TextView userSubPlanText;
    private CircularNetworkImageView userPhoto;

    ImageLoader imageLoader = AppController.getInstance().getImageLoader();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Profile");

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

        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        userNameText = (TextView) view.findViewById(R.id.userNameText);
        userEmailText = (TextView) view.findViewById(R.id.userEmailText);
        userAccountTypeText = (TextView) view.findViewById(R.id.userAccountType);
        userSubPlanText = (TextView) view.findViewById(R.id.userSubPlan);
        userPhoto = (CircularNetworkImageView) view.findViewById(R.id.userPhoto);

        getUserInfo(false);

        return view;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_user_profile_fragment, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_view_all_info) {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        getUserInfo(false);
    }

    private void showInfo(){
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(JSON_STRING);
            JSONArray result = jsonObject.getJSONArray("user");

            JSONObject jo = result.getJSONObject(0);
            String userName = jo.getString("userName");
            String userEmail = jo.getString("userEmail");
            String userAccountType = jo.getString("userAccountType");
            String userSubPlan = jo.getString("userSubPlan");
            String userPhoto = jo.getString("userPhoto");

            userNameText.setText(userName);
            userEmailText.setText("Email: " + userEmail);
            userAccountTypeText.setText(userAccountType);
            userSubPlanText.setText("Subscription: " + userSubPlan);
            this.userPhoto.setImageUrl(userPhoto, imageLoader);

        } catch (JSONException e) {
            e.printStackTrace();
        }

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
     * Function to get all of user info from MySQL DB
     * */
    private void getUserInfo(final boolean getAccountInfo) {
        // Tag used to cancel the request
        String tag_string_req = "req_get_user_info";

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_GET_USER_INFO, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Get User Response: " + response.toString());
                //hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        JSON_STRING = response;
                        showInfo();
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

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Fetching Error: " + error.getMessage());
                Toast.makeText(getActivity(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                //hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("userID", userID);

                if(getAccountInfo){
                    params.put("accountID", accountID);
                }

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

}
