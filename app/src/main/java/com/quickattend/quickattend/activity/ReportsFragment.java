package com.quickattend.quickattend.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.app.ProgressDialog;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import static java.lang.Float.NaN;

public class ReportsFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private ProgressDialog pDialog;

    private String JSON_STRING;
    private SQLiteHandler db;
    private SessionManager session;
    private String userID;
    private String accountID;

    String currentDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());

    BarChart chart;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Progress dialog
        pDialog = new ProgressDialog(getActivity());
        pDialog.setCancelable(false);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Reports: " + currentDate);

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
        String email = user.get("email");

        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        chart = (BarChart) view.findViewById(R.id.barChart);
        //chart.saveToGallery("First Chart", 100);
        getChartAttendance();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getChartAttendance();
    }

    private void showChart() {
        JSONObject jsonObject = null;

        ArrayList<BarEntry> entries = new ArrayList<>();
        // the labels that should be drawn on the XAxis
        String[] classNames = new String[5];

        try {
            jsonObject = new JSONObject(JSON_STRING);
            JSONArray result = jsonObject.getJSONArray("classes");

            for (int i = 0; i < result.length(); i++) {
                JSONObject jo = result.getJSONObject(i);
                String className = jo.getString("className");
                String classAttendance = jo.getString("averageAttendance");

                entries.add(new BarEntry(i, Integer.parseInt(classAttendance)));
                classNames[i] = className;
            }

            final String[] thisClassNames = classNames;

            IAxisValueFormatter formatter = new IAxisValueFormatter() {

                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    return thisClassNames[(int) value];
                }

                // we don't draw numbers, so no decimal digits needed
                public int getDecimalDigits() {
                    return 0;
                }
            };

            XAxis xAxis = chart.getXAxis();
            xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
            xAxis.setValueFormatter(formatter);

            BarDataSet dataset = new BarDataSet(entries, "Attendance");
            Legend legend = chart.getLegend();

            dataset.setColors(new int[]{R.color.bar1, R.color.bar2, R.color.bar3, R.color.bar4, R.color.bar5}, getActivity());
            dataset.setValueTextSize(12f);
            LegendEntry legendEntry0 = new LegendEntry(thisClassNames[0], Legend.LegendForm.DEFAULT, NaN, NaN, null, ContextCompat.getColor(getActivity(), R.color.bar1));
            LegendEntry legendEntry1 = new LegendEntry(thisClassNames[1], Legend.LegendForm.DEFAULT, NaN, NaN, null, ContextCompat.getColor(getActivity(), R.color.bar2));
            LegendEntry legendEntry2 = new LegendEntry(thisClassNames[2], Legend.LegendForm.DEFAULT, NaN, NaN, null, ContextCompat.getColor(getActivity(), R.color.bar3));
            LegendEntry legendEntry3 = new LegendEntry(thisClassNames[3], Legend.LegendForm.DEFAULT, NaN, NaN, null, ContextCompat.getColor(getActivity(), R.color.bar4));
            LegendEntry legendEntry4 = new LegendEntry(thisClassNames[4], Legend.LegendForm.DEFAULT, NaN, NaN, null, ContextCompat.getColor(getActivity(), R.color.bar5));

            legend.setCustom(new LegendEntry[]{legendEntry0, legendEntry1, legendEntry2, legendEntry3, legendEntry4});

            BarData barData = new BarData(dataset);
            Description chartDescription = new Description();
            chartDescription.setText("Attendance Chart");
            chart.setData(barData);
            chart.setDescription(chartDescription);
            chart.animateY(2000, Easing.EasingOption.EaseInOutElastic);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
    }


    /**
     * Function to get account's average class attendance from MySQL DB
     */
    private void getChartAttendance() {
        // Tag used to cancel the request
        String tag_string_req = "req_get_chart_attendance";

        pDialog.setMessage("Loading Chart...");
        //showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_GET_CHART_ATTENDANCE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Get Chart Data Response: " + response.toString());
                //hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        JSON_STRING = response;
                        showChart();
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
