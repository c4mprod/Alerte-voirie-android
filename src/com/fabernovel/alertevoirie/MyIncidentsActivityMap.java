package com.fabernovel.alertevoirie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.Incident;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.utils.SimpleItemizedOverlay;
import com.fabernovel.alertevoirie.utils.Utils;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class MyIncidentsActivityMap extends MapActivity implements RequestListener {

    public static final String[] INCIDENTS       = new String[] { JsonData.PARAM_DECLARED_INCIDENTS, JsonData.PARAM_UPDATED_INCIDENTS,
            JsonData.PARAM_RESOLVED_INCIDENTS   };
    private static final int     DIALOG_PROGRESS = 123456789;
    JSONObject                   data            = new JSONObject();
    ToggleButton                 tbmap;
    RadioGroup                   tabs;
    protected int                checked;
    String[]                     title           = new String[3];
    private MapView              map;
    int                          lat_min         = 999999999;
    int                          lat_max         = 0;
    int                          lon_min         = 999999999;
    int                          lon_max         = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_report_map);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_mes_rapports);

        map = (MapView) findViewById(R.id.MapView_mymap);
        map.setBuiltInZoomControls(true);
        tabs = (RadioGroup) findViewById(R.id.RadioGroup_tabs_map);
        tbmap = (ToggleButton) findViewById(R.id.ToggleButton_Map);
        tbmap.setChecked(true);
        tbmap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    tbmap.setChecked(true);
                    Intent i = new Intent(MyIncidentsActivityMap.this, MyIncidentsActivity.class);
                    i.putExtra("tab1", title[0]);
                    i.putExtra("tab2", title[1]);
                    i.putExtra("tab3", title[2]);
                    i.putExtra("datas", data.toString());
                    i.putExtra("tab", checked);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(i);
                }

            }
        });

        if (getIntent().getExtras().getInt("tab") != 0) {

            title[0] = getIntent().getExtras().getString("tab1");
            title[1] = getIntent().getExtras().getString("tab2");
            title[2] = getIntent().getExtras().getString("tab3");
            ((TextView) tabs.getChildAt(0)).setText(title[0]);
            if (title[0].startsWith("0")) ((TextView) tabs.getChildAt(0)).setEnabled(false);
            ((TextView) tabs.getChildAt(1)).setText(title[1]);
            if (title[1].startsWith("0")) ((TextView) tabs.getChildAt(1)).setEnabled(false);
            ((TextView) tabs.getChildAt(2)).setText(title[2]);
            if (title[2].startsWith("0")) ((TextView) tabs.getChildAt(2)).setEnabled(false);

            checked = getIntent().getExtras().getInt("tab");

            try {
                data = new JSONObject(getIntent().getExtras().getString("datas"));
            } catch (JSONException e) {
                Log.e(Constants.PROJECT_TAG, "JSon data exception", e);
            }

            // setMapForTab(gettabIndex(tabs.getCheckedRadioButtonId()));

        } else {

            // launch request
            try {
                AVService.getInstance(this).postJSON(new JSONArray().put(new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_GET_MY_INCIDENTS)
                                                                                         .put(JsonData.PARAM_UDID, Utils.getUdid(this))), this);
                showDialog(DIALOG_PROGRESS);
            } catch (JSONException e) {
                Log.e(Constants.PROJECT_TAG, "error launching My Incidents", e);
            }
        }

        // get view references
        tabs.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(Constants.PROJECT_TAG, "checked : " + checkedId);
                checked = checkedId;
                setMapForTab(gettabIndex(checkedId));
            }
        });

        tabs.check(getId());

        if (checked == R.id.Tab_Map_ongoing) {
            setMapForTab(checked);
        }

    }

    @Override
    protected void onPause() {
        // Animation animSlide = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        // animSlide.setDuration(200);

        // startAnimation(animSlide);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        super.onPause();
    }

    @Override
    protected void onStop() {

        super.onStop();

    }

    private int getId() {
        switch (checked) {
            case R.id.Tab_ongoing:
                checked = R.id.Tab_Map_ongoing;

                break;
            case R.id.Tab_resolved:
                checked = R.id.Tab_Map_resolved;

                break;
            case R.id.Tab_updated:
                checked = R.id.Tab_Map_updated;

                break;

            default:
                checked = R.id.Tab_Map_ongoing;
                break;
        }
        return checked;
    }

    protected void setMapForTab(int gettabIndex) {
        map.getOverlays().clear();
        JSONArray datas = null;
        try {
            datas = data.getJSONArray(INCIDENTS[gettabIndex]);

        } catch (Exception e) {
            Log.e(Constants.PROJECT_TAG, "Exception", e);
            try {
                datas = data.getJSONArray(INCIDENTS[0]);
            } catch (JSONException e1) {
                Log.e(Constants.PROJECT_TAG, "JSONException in setMapForTab", e1);
            }
        }

        lat_min = 999999999;
        lat_max = 0;
        lon_min = 999999999;
        lon_max = 0;

        if (datas != null) {
            for (int i = 0; i < datas.length(); i++) {
                try {

                    Incident myIncident = Incident.fromJSONObject(datas.getJSONObject(i));

                    if (lat_min > (myIncident.latitude * 1E6)) lat_min = (int) (myIncident.latitude * 1E6);
                    if (lat_max < (myIncident.latitude * 1E6)) lat_max = (int) (myIncident.latitude * 1E6);

                    if (lon_min > (myIncident.longitude * 1E6)) lon_min = (int) (myIncident.longitude * 1E6);
                    if (lon_max < (myIncident.longitude * 1E6)) lon_max = (int) (myIncident.longitude * 1E6);

                    GeoPoint geo = new GeoPoint((int) (myIncident.latitude * 1E6), (int) (myIncident.longitude * 1E6));
                    setMarker(geo, myIncident);
                } catch (JSONException e) {
                    Log.e(Constants.PROJECT_TAG, "Marker error", e);
                }

            }

            if (lat_min != 0) {

                if (map.getLatitudeSpan() < (lat_max - lat_min)) {
                    map.getController().zoomToSpan(lat_max - lat_min, lon_max - lon_min);
                    map.getController().animateTo(new GeoPoint(lat_min + (lat_max - lat_min) / 2, lon_min + (lon_max - lon_min) / 2));
                } else {
                    map.getController().animateTo(new GeoPoint(lat_min + (lat_max - lat_min) / 2, lon_min + (lon_max - lon_min) / 2), new Runnable() {

                        @Override
                        public void run() {
                            map.getController().zoomToSpan(lat_max - lat_min, lon_max - lon_min);

                        }
                    });
                }
            }
        }
    }

    private void setMarker(final GeoPoint newGeo, Incident incident) {

        SimpleItemizedOverlay cursor = new SimpleItemizedOverlay(getResources().getDrawable(R.drawable.map_cursor));
        cursor.addOverlayItem(new OverlayItem(newGeo, incident.date, incident.description));
        map.getOverlays().add(cursor);
        map.setSatellite(false);
        map.invalidate();

    }

    private int gettabIndex(int id) {
        switch (id) {
            case R.id.Tab_Map_ongoing:
                return 0;
            case R.id.Tab_Map_updated:
                return 1;
            case R.id.Tab_Map_resolved:
                return 2;
            default:
                return -1;
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void onRequestcompleted(int requestCode, Object result) {
        // TODO Auto-generated method stub

    }

}
