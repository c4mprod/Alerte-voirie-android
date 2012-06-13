/**
 * This file is part of the Alerte Voirie project.
 * 
 * Copyright (C) 2010-2011 C4M PROD
 * 
 * Alerte Voirie is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alerte Voirie is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alerte Voirie.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.fabernovel.alertevoirie;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.Incident;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.entities.Last_Location;
import com.fabernovel.alertevoirie.utils.SimpleItemizedOverlay;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class IncidentsActivityMap extends MapActivity implements RequestListener {

    public static final String[] INCIDENTS       = new String[] { JsonData.PARAM_ONGOING_INCIDENTS, JsonData.PARAM_UPDATED_INCIDENTS,
            JsonData.PARAM_RESOLVED_INCIDENTS   };
    private static final int     DIALOG_PROGRESS = 123456789;
    JSONArray[]                  data            = new JSONArray[3];
    RadioGroup                   tabs;
    protected int                checked;
    String[]                     title           = new String[3];
    private MapView              map;
    ArrayList<Incident>          ongoing         = new ArrayList<Incident>();
    ArrayList<Incident>          updated         = new ArrayList<Incident>();
    ArrayList<Incident>          resolved        = new ArrayList<Incident>();
    int                          lat_min         = Integer.MAX_VALUE;
    int                          lat_max         = 0;
    int                          lon_min         = Integer.MAX_VALUE;
    int                          lon_max         = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_report_map);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_autour_de_vous);

        map = (MapView) findViewById(R.id.MapView_mymap);
        tabs = (RadioGroup) findViewById(R.id.RadioGroup_tabs_map);

        findViewById(R.id.ToggleButton_Map).setVisibility(View.GONE);

        for (int i = 0; i < 3; i++) {
            int quantity = 0;
            String stitle = null;
            switch (i) {
                case 0:
                    stitle = getResources().getQuantityString(R.plurals.home_label_current, quantity, quantity);
                    break;
                case 1:
                    stitle = getResources().getQuantityString(R.plurals.home_label_update, quantity, quantity);
                    break;
                case 2:
                    stitle = getResources().getQuantityString(R.plurals.home_label_solved, quantity, quantity);
                    break;

                default:
                    break;
            }
            title[i] = stitle;
            ((TextView) tabs.getChildAt(i)).setText(title[i]);
            ((TextView) tabs.getChildAt(i)).setEnabled(false);

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

        /*
         * if (checked == R.id.Tab_Map_ongoing) {
         * setMapForTab(checked);
         * }
         */

    }

    @Override
    protected void onStart() {
        super.onStart();
        // refresh map (important doing this here because of other mapview in next screens)
        map.setSatellite(false);
        map.invalidate();
        map.getOverlays().clear();
        data = new JSONArray[3];
        ongoing.clear();
        updated.clear();
        resolved.clear();

        // launch request
        try {
            showDialog(DIALOG_PROGRESS);
            JSONObject request = new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_GET_INCIDENTS_BY_POSITION)
                                                 .put(JsonData.PARAM_RADIUS, JsonData.VALUE_RADIUS_FAR)
                                                 .put(JsonData.PARAM_POSITION,
                                                      new JSONObject().put(JsonData.PARAM_POSITION_LONGITUDE, Last_Location.longitude)
                                                                      .put(JsonData.PARAM_POSITION_LATITUDE, Last_Location.latitude));

            AVService.getInstance(this).postJSON(new JSONArray().put(request), this);

        } catch (Exception e) {
            Log.e(Constants.PROJECT_TAG, "Eror retrieving incidents", e);
        }

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
        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

        ArrayList<Incident> datas = new ArrayList<Incident>();
        switch (gettabIndex) {
            case 0:
                datas = ongoing;

                break;
            case 1:
                datas = updated;
                break;
            case 2:
                datas = resolved;
                break;

            default:
                break;
        }
        map.getOverlays().clear();

        for (Incident myIncident : datas) {

            items.add(myIncident);

            int quantity = datas.size();
            String title = null;
            switch (gettabIndex) {
                case 0:
                    title = getResources().getQuantityString(R.plurals.home_label_current, quantity, quantity);
                    break;
                case 1:
                    title = getResources().getQuantityString(R.plurals.home_label_update, quantity, quantity);
                    break;
                case 2:
                    title = getResources().getQuantityString(R.plurals.home_label_solved, quantity, quantity);
                    break;

                default:
                    break;
            }

            ((TextView) tabs.getChildAt(gettabIndex)).setText(title);
            if (quantity == 0) ((TextView) tabs.getChildAt(gettabIndex)).setEnabled(false);

        }

        lat_min = Integer.MAX_VALUE;
        lat_max = 0;
        lon_min = Integer.MAX_VALUE;
        lon_max = 0;

        if (datas != null && datas.size() > 0) {
            for (int i = 0; i < datas.size(); i++) {

                Incident myIncident = (datas.get(i));

                if (lat_min > (myIncident.latitude * 1E6)) lat_min = (int) (myIncident.latitude * 1E6);
                if (lat_max < (myIncident.latitude * 1E6)) lat_max = (int) (myIncident.latitude * 1E6);

                if (lon_min > (myIncident.longitude * 1E6)) lon_min = (int) (myIncident.longitude * 1E6);
                if (lon_max < (myIncident.longitude * 1E6)) lon_max = (int) (myIncident.longitude * 1E6);

                items.add(myIncident);

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
        } else {
            // center on marseille
            map.getController().setZoom(14);
            map.getController().setCenter(new GeoPoint(43297608, 5381018));
        }

        SimpleItemizedOverlay overlay = new SimpleItemizedOverlay(getResources().getDrawable(R.drawable.map_cursor), this, map, items);
        map.getOverlays().add(overlay);
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
        Log.d(Constants.PROJECT_TAG, "resp : " + result);

//@formatter:off
/**
        [
            {
                "answer": {
                    "closest_incidents": [
                        {
                            "incidentObj": {
                                "descriptive": "Barri\u00e8res de travaux \u00f4ter", 
                                "pictures": {
                                    "far": [
                                        "http://alerte-voirie.ppd.c4mprod.com//media/collection_image/2010/08/17/2010-08-17_0.518940819845_picture.jpg",
                                        "http://alerte-voirie.ppd.c4mprod.com//media/collection_image/2010/08/16/2010-08-16_0.890694847535_picture.jpg"
                                    ],
                                    "close": [
                                        "http://alerte-voirie.ppd.c4mprod.com//media/collection_image/2010/08/17/2010-08-17_0.248174823475_picture.jpg", 
                                        "http://alerte-voirie.ppd.c4mprod.com//media/collection_image/2010/08/17/2010-08-17_0.33014646727_picture.jpg"
                                    ]
                                }, 
                                "categoryId": 27,
                                "date": "2010-08-16 15:28:24.740044", 
                                "state": "U", 
                                "address": "36 Quai du lazaret\n13002 Marseille", 
                                "lat": 43.309444427490227, 
                                "lng": 5.3668661117553711, 
                                "confirms": 1, 
                                "id": 27, 
                                "invalidations": 0
                            }
                        }, 
                        {
                            "incidentObj": {
                                "descriptive": "Super t\u00e9l\u00e9phone", 
                                "pictures": {
                                    "far": [
                                        "http://alerte-voirie.ppd.c4mprod.com//media/collection_image/2010/09/21/2010-09-21_0.86314714662_picture.jpg"
                                    ], 
                                    "close": [
                                        "http://alerte-voirie.ppd.c4mprod.com//media/collection_image/2010/09/21/2010-09-21_0.261345643559_picture.jpg"
                                    ]
                                }, 
                                "categoryId": 33, 
                                "date": "2010-09-21 11:05:16.760450", 
                                "state": "O", 
                                "address": "45 L'Autoroute du Littoral\n13002 Marseille", 
                                "lat": 43.317855834960938, 
                                "lng": 5.3635425567626953, 
                                "confirms": 0, 
                                "id": 34, 
                                "invalidations": 0
                            }
                        }
                    ], 
                    "status": 0
                },
                "request": "getIncidentsByPosition"
            }
        ]
*/
//@formatter:on 

        try {
            JSONArray responses;
            responses = new JSONArray((String) result);
            JSONObject response = responses.getJSONObject(0);

            if (requestCode == AVService.REQUEST_JSON) {
                if (JsonData.VALUE_REQUEST_GET_INCIDENTS_BY_POSITION.equals(response.getString(JsonData.PARAM_REQUEST))) {
                    JSONArray items = response.getJSONObject(JsonData.PARAM_ANSWER).getJSONArray(JsonData.PARAM_CLOSEST_INCIDENTS);

                    for (int i = 0; i < items.length(); i++) {
                        Incident incident = Incident.fromJSONObject(this, items.getJSONObject(i));
                        switch (items.getJSONObject(i).getString(JsonData.PARAM_INCIDENT_STATUS).charAt(0)) {
                            case 'O':
                                ongoing.add(incident);
                                break;
                            case 'U':
                                updated.add(incident);
                                break;
                            case 'R':
                                resolved.add(incident);
                                break;

                            default:
                                break;
                        }
                    }
                    setMapForTab(gettabIndex(checked));

                    ArrayList<Incident> datas = null;
                    String title = null;

                    for (int i = 0; i < 3; i++) {

                        switch (i) {
                            case 0: {
                                datas = ongoing;
                                int quantity = datas.size();
                                title = getResources().getQuantityString(R.plurals.home_label_current, quantity, quantity);
                                break;
                            }
                            case 1: {
                                datas = updated;
                                int quantity = datas.size();
                                title = getResources().getQuantityString(R.plurals.home_label_update, quantity, quantity);
                                break;
                            }
                            case 2: {
                                datas = resolved;
                                int quantity = datas.size();
                                title = getResources().getQuantityString(R.plurals.home_label_solved, quantity, quantity);
                                break;
                            }
                            default:
                                datas = new ArrayList<Incident>();
                                title = "0";
                                break;
                        }

                        ((TextView) tabs.getChildAt(i)).setText(title);

                        if (!title.startsWith("0")) ((TextView) tabs.getChildAt(i)).setEnabled(true);

                    }
                }
            }

        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "error in onRequestcompleted : Json", e);
            Toast.makeText(this, "Erreur serveur", Toast.LENGTH_LONG).show();
        } catch (ClassCastException e) {
            Log.e(Constants.PROJECT_TAG, "error in onRequestcompleted : ClasscastException", e);
            Toast.makeText(this, "Erreur serveur", Toast.LENGTH_LONG).show();

        }

        // dismissDialog(DIALOG_PROGRESS);
    }
}
