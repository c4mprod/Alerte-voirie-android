package com.fabernovel.alertevoirie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.utils.LocationHelper;
import com.fabernovel.alertevoirie.utils.Utils;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;
import com.google.android.maps.GeoPoint;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class HomeActivity extends Activity implements OnClickListener, LocationListener, RequestListener {
    private static final int DIALOG_PROGRESS = 0;
    private LocationManager  locationManager;
    private Location         lastlocation;
    private static boolean   handled         = false;
    private boolean dialog_shown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showDialog(DIALOG_PROGRESS);
        dialog_shown = true;

        setContentView(R.layout.layout_home);

        // init buttons
        findViewById(R.id.Button_news).setOnClickListener(this);
        findViewById(R.id.Button_reports).setOnClickListener(this);
        findViewById(R.id.Button_new_incident).setOnClickListener(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        lastlocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!handled) {
                    AVService.getInstance(HomeActivity.this).toastServerError(HomeActivity.this.getString(R.string.gps_error));
                    locationManager.removeUpdates(HomeActivity.this);
                    handleNewLocation(lastlocation);
                }
            }
        }, 30000);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Button_news:
                startActivity(new Intent(this, NewsActivity.class));
                // startActivity(new Intent(this, ExistingIncidentsActivity.class));
                break;
            case R.id.Button_reports:
                startActivity(new Intent(this, MyIncidentsActivity.class));
                break;
            case R.id.Button_new_incident:
                Intent i = new Intent(this, ExistingIncidentsActivity.class);
                i.putExtra(Constants.NEW_REPORT, true);
                startActivity(i);
                // startActivity(new Intent(this, ReportDetailsActivity.class));
                break;

            default:
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (LocationHelper.isBetterLocation(location, lastlocation)) {
            handleNewLocation(location);
            locationManager.removeUpdates(this);
            handled = true;
        }

    }

    private void handleNewLocation(Location location) {
        lastlocation = location;

        try {
            GeoPoint newGeo = LocationHelper.geoFromLocation(lastlocation);

            JSONObject request = new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_GET_INCIDENTS_STATS)
                                                 .put(JsonData.PARAM_UDID, Utils.getUdid(this))
                                                 .put(JsonData.PARAM_POSITION,
                                                      new JSONObject().put(JsonData.PARAM_POSITION_LONGITUDE, newGeo.getLongitudeE6() / 1E6)
                                                                      .put(JsonData.PARAM_POSITION_LATITUDE, newGeo.getLatitudeE6() / 1E6));

            AVService.getInstance(this).postJSON(new JSONArray().put(request), this);

        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "Error loading existing incidents", e);
            // dismissDialog(DIALOG_PROGRESS);
        } catch (NullPointerException e) {
            Log.e(Constants.PROJECT_TAG, "Nullpointer Error", e);
            // dismissDialog(DIALOG_PROGRESS);
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
        // TODO Auto-generated method stub

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
                        if(dialog_shown)
                        removeDialog(DIALOG_PROGRESS);
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
            responses = new JSONArray((String) result);
            JSONObject response = responses.getJSONObject(0);

            if (requestCode == AVService.REQUEST_JSON) {
                if (JsonData.VALUE_REQUEST_GET_INCIDENTS_STATS.equals(response.getString(JsonData.PARAM_REQUEST))) {
                    int resolved_incidents = response.getJSONObject(JsonData.PARAM_ANSWER).getInt(JsonData.PARAM_RESOLVED_INCIDENTS);
                    int ongoing_incidents = response.getJSONObject(JsonData.PARAM_ANSWER).getInt(JsonData.PARAM_ONGOING_INCIDENTS);
                    int updated_incidents = response.getJSONObject(JsonData.PARAM_ANSWER).getInt(JsonData.PARAM_UPDATED_INCIDENTS);

                    if (resolved_incidents > 2) {
                        ((TextView) findViewById(R.id.Home_TextView_Solved)).setText(((String) ((TextView) findViewById(R.id.Home_TextView_Solved)).getText()).replace("u",
                                                                                                                                                                       "us"));
                    }

                    ((TextView) findViewById(R.id.Home_TextView_Solved_Number)).setText("" + resolved_incidents);
                    ((TextView) findViewById(R.id.Home_TextView_Current_Number)).setText("" + ongoing_incidents);
                    ((TextView) findViewById(R.id.Home_TextView_Update_Number)).setText("" + updated_incidents);

                }
            }
        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "JSONException", e);
        } catch (ClassCastException e) {
            Log.e(Constants.PROJECT_TAG, "Invalid result. Trying to cast " + result.getClass() + "into String", e);
        } finally {
            dismissDialog(DIALOG_PROGRESS);
            dialog_shown = false;
        }
    }

    @Override
    protected void onResume() {
        handleNewLocation(lastlocation);
        super.onResume();
    }
}
