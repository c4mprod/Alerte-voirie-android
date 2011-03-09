package com.fabernovel.alertevoirie.entities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.fabernovel.alertevoirie.R;
import com.fabernovel.alertevoirie.utils.Utils;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class Incident extends OverlayItem {
    public Incident(GeoPoint point, String title, String snippet) {
        super(point, title, snippet);
        // TODO Auto-generated constructor stub
    }

    public Incident() {
        this(null, null, null);
    }

    public static final char STATUS_ONGOING  = 'O';
    public static final char STATUS_RESOLVED = 'R';
    public static final char STATUS_UPDATED  = 'U';

    public String            address;
    public String            description;
    public long              categoryId;
    public double            latitude;
    public double            longitude;
    public String            date;
    public char              state;
    public int               confirms;
    public long              id;
    public JSONArray         pictures_far;
    public JSONArray         pictures_close;
    public int               invalidations;
    public JSONObject        json;

    public JSONObject getNewIncidentRequest(Context c) {
        try {
            return new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_NEW_INCIDENT)
                                   .put(JsonData.PARAM_UDID, c != null ? Utils.getUdid(c) : "0000")
                                   .put(JsonData.PARAM_INCIDENT,
                                        new JSONObject().put(JsonData.PARAM_INCIDENT_CATEGORY, categoryId)
                                                        .put(JsonData.PARAM_INCIDENT_ADDRESS, address)
                                                        .put(JsonData.PARAM_INCIDENT_DESCRIPTION, description))
                                   .put(JsonData.PARAM_POSITION,
                                        new JSONObject().put(JsonData.PARAM_POSITION_LATITUDE, latitude).put(JsonData.PARAM_POSITION_LONGITUDE, longitude));
        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "Error creating new incident", e);
            return null;
        }
    }
    
    public JSONObject getChangeIncidentRequest(Context c) {
        try {
            return new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_CHANGE_INCIDENT)
            .put(JsonData.PARAM_IMAGES_INCIDENT_ID, id)
            .put(JsonData.PARAM_INCIDENT_CATEGORY, categoryId)
            .put(JsonData.PARAM_INCIDENT_ADDRESS, address);
        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "Error creating new incident", e);
            return null;
        }
    }

    public JSONObject updateIncidentRequest(Context c, String status) {

        if (Constants.DEBUGMODE) Log.d(Constants.PROJECT_TAG, "updateIncidentRequest : ");
        try {
            return new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_UPDATE_INCIDENT)
                                   .put(JsonData.PARAM_UPDATE_INCIDENT_LOG,
                                        new JSONObject().put(JsonData.ANSWER_INCIDENT_ID, id)
                                                        .put(JsonData.PARAM_UDID, Utils.getUdid(c))
                                                        .put(JsonData.PARAM_STATUS, status));
        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "Error updating incident", e);
            return null;
        }
    }

    public static Incident fromJSONObject(Context c, JSONObject obj) {
        try {
            Log.d(Constants.PROJECT_TAG, obj.toString());

            double latitude = obj.getDouble(JsonData.PARAM_INCIDENT_LATITUDE);
            double longitude = obj.getDouble(JsonData.PARAM_INCIDENT_LONGITUDE);
            String name = obj.getString(JsonData.PARAM_INCIDENT_DESCRIPTION);

            Incident result = new Incident(new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6)), name, "snippet");
            result.json = obj;
            result.latitude = latitude;
            result.longitude = longitude;
            result.address = obj.getString(JsonData.PARAM_INCIDENT_ADDRESS);
            result.description = name;
            result.date = obj.getString(JsonData.PARAM_INCIDENT_DATE);
            result.state = obj.getString(JsonData.PARAM_INCIDENT_STATUS).charAt(0);
            result.categoryId = obj.getLong(JsonData.PARAM_INCIDENT_CATEGORY);

            result.confirms = obj.getInt(JsonData.PARAM_INCIDENT_CONFIRMS);
            result.invalidations = obj.getInt(JsonData.PARAM_INCIDENT_INVALIDATION);
            result.id = obj.getLong(JsonData.PARAM_INCIDENT_ID);
            result.pictures_far = obj.getJSONObject(JsonData.PARAM_INCIDENT_PICTURES).getJSONArray(JsonData.PARAM_INCIDENT_PICTURES_FAR);
            result.pictures_close = obj.getJSONObject(JsonData.PARAM_INCIDENT_PICTURES).getJSONArray(JsonData.PARAM_INCIDENT_PICTURES_CLOSE);

            result.setMarker(c.getResources().getDrawable(R.drawable.map_cursor));
            return result;
        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "Can't create Incident", e);
            return null;
        }
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return json.toString();
    }
}
