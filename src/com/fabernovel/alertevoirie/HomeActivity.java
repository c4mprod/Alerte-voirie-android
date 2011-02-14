package com.fabernovel.alertevoirie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;
import android.widget.TextView;

import com.c4mprod.utils.Flip3dAnimation;
import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.entities.Last_Location;
import com.fabernovel.alertevoirie.utils.LocationHelper;
import com.fabernovel.alertevoirie.utils.Utils;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;

public class HomeActivity extends Activity implements OnClickListener, LocationListener, RequestListener {
    private static final int DIALOG_PROGRESS = 0;
    private LocationManager  locationManager;
    private Location         lastlocation;
    private boolean          dialog_shown    = false;
    private boolean          hidedialog      = false;
    private Handler          myHandler       = new Handler();
    private Runnable         removeUpdate    = new Runnable() {
                                                 @Override
                                                 public void run() {
                                                     AVService.getInstance(HomeActivity.this).toastServerError(HomeActivity.this.getString(R.string.gps_error));
                                                     locationManager.removeUpdates(HomeActivity.this);
                                                     handleNewLocation(lastlocation);
                                                 }
                                             };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialog_shown = true;
        showDialog(DIALOG_PROGRESS);

        setContentView(R.layout.layout_home);

        // init buttons
        findViewById(R.id.Button_news).setOnClickListener(this);
        findViewById(R.id.Button_reports).setOnClickListener(this);
        findViewById(R.id.Button_new_incident).setOnClickListener(this);
        findViewById(R.id.Button_incidents).setOnClickListener(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        lastlocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

        myHandler.postDelayed(removeUpdate, 30000);

    }

    @Override
    protected void onPause() {
        this.hidedialog = false;
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Button_news:
                startActivity(new Intent(this, NewsActivity.class));
                break;
            case R.id.Button_reports:
                startActivity((new Intent(this, MyIncidentsActivity.class)));
                break;
            case R.id.Button_incidents:
                startActivity((new Intent(this, IncidentsActivityMap.class)));
                break;
            case R.id.Button_new_incident:
                Intent i = new Intent(this, ExistingIncidentsActivity.class);
                i.putExtra(Constants.NEW_REPORT, true);
                startActivity(i);
                break;

            default:
                break;
        }
        // overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (LocationHelper.isBetterLocation(location, lastlocation)) {
            handleNewLocation(location);
            locationManager.removeUpdates(this);
            myHandler.removeCallbacks(removeUpdate);
        }

    }

    private void handleNewLocation(Location location) {
        lastlocation = location;

        try {

            Last_Location.longitude = location.getLongitude();
            Last_Location.latitude = location.getLatitude();

            JSONObject request = new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_GET_INCIDENTS_STATS)
                                                 .put(JsonData.PARAM_UDID, Utils.getUdid(this))
                                                 .put(JsonData.PARAM_POSITION,
                                                      new JSONObject().put(JsonData.PARAM_POSITION_LONGITUDE, Last_Location.longitude)
                                                                      .put(JsonData.PARAM_POSITION_LATITUDE, Last_Location.latitude));
            AVService.getInstance(this).postJSON(new JSONArray().put(request), this);
        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "Error loading existing incidents", e);
        } catch (NullPointerException e) {
            Log.e(Constants.PROJECT_TAG, "Nullpointer Error", e);
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderDisabled(String provider) {
        AVService.getInstance(HomeActivity.this).toastServerError(HomeActivity.this.getString(R.string.gps_error));
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PROGRESS:
                ProgressDialog pd = new ProgressDialog(this);
                pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                pd.setIndeterminate(true);
                pd.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (dialog_shown) removeDialog(DIALOG_PROGRESS);
                    }
                });
                pd.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        AVService.getInstance(HomeActivity.this).cancelTask();
                        finish();
                    }
                });
                pd.setMessage(getString(R.string.ui_message_loading));
                return pd;

            default:
                return super.onCreateDialog(id);
        }
    }

    @Override
    public void onRequestcompleted(int requestCode, Object result) {
        try {
            JSONArray responses;
            Log.d(Constants.PROJECT_TAG, (String) result);
            responses = new JSONArray((String) result);

            JSONObject response = responses.getJSONObject(0);

            final AnimationSet set = new AnimationSet(false);

            final float centerX = findViewById(R.id.LinearLayout02).getWidth() / 2;
            final float centerY = findViewById(R.id.LinearLayout02).getHeight() / 2;
            final Flip3dAnimation animation = new Flip3dAnimation(0, 360, centerX, centerY);
            animation.setDuration(500);
            set.setFillAfter(true);
            set.setFillBefore(true);
            animation.setInterpolator(new AccelerateInterpolator());

            set.addAnimation(animation);

            if (requestCode == AVService.REQUEST_JSON) {
                if (JsonData.VALUE_REQUEST_GET_INCIDENTS_STATS.equals(response.getString(JsonData.PARAM_REQUEST))) {
                    Last_Location.Incidents = response.getJSONObject(JsonData.PARAM_ANSWER);
                    int resolved_incidents = response.getJSONObject(JsonData.PARAM_ANSWER).getInt(JsonData.PARAM_RESOLVED_INCIDENTS);
                    int ongoing_incidents = response.getJSONObject(JsonData.PARAM_ANSWER).getInt(JsonData.PARAM_ONGOING_INCIDENTS);
                    int updated_incidents = response.getJSONObject(JsonData.PARAM_ANSWER).getInt(JsonData.PARAM_UPDATED_INCIDENTS);

                    if (resolved_incidents > 2) {
                        ((TextView) findViewById(R.id.Home_TextView_Solved)).setText(((String) ((TextView) findViewById(R.id.Home_TextView_Solved)).getText()).replace("us",
                                                                                                                                                                       "u")
                                                                                                                                                              .replace("u",
                                                                                                                                                                       "us"));
                    }

                    ((TextView) findViewById(R.id.Home_TextView_Solved_Number)).setText("" + resolved_incidents);
                    ((TextView) findViewById(R.id.Home_TextView_Current_Number)).setText("" + ongoing_incidents);
                    ((TextView) findViewById(R.id.Home_TextView_Update_Number)).setText("" + updated_incidents);

                }
            }

            findViewById(R.id.LinearLayout02).startAnimation(set);
            findViewById(R.id.LinearLayout04).startAnimation(set);
            findViewById(R.id.LinearLayout03).startAnimation(set);
        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "JSONException", e);
        } catch (ClassCastException e) {
            Log.e(Constants.PROJECT_TAG, "Invalid result. Trying to cast " + result.getClass() + "into String", e);
        } finally {
            if (hidedialog) dismissDialog(DIALOG_PROGRESS);
            dialog_shown = false;
        }
    }

    @Override
    protected void onResume() {
        hidedialog = true;
        handleNewLocation(lastlocation);
        super.onResume();
    }
}
