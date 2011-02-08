package com.fabernovel.alertevoirie.entities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.fabernovel.alertevoirie.utils.Utils;

public class Incident {
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
    public static JSONObject json;

    public JSONObject getNewIncidentRequest(Context c) {
        try {
            return new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_NEW_INCIDENT)
                                   .put(JsonData.PARAM_UDID, c != null ? Utils.getUdid(c) : "0000")
                                   .put(JsonData.PARAM_INCIDENT,
                                        new JSONObject().put(JsonData.PARAM_INCIDENT_CATEGORY, categoryId).put(JsonData.PARAM_INCIDENT_ADDRESS, address)
                                                        .put(JsonData.PARAM_INCIDENT_DESCRIPTION, description))
                                   .put(JsonData.PARAM_POSITION,
                                        new JSONObject().put(JsonData.PARAM_POSITION_LATITUDE, latitude).put(JsonData.PARAM_POSITION_LONGITUDE, longitude));
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
                                        new JSONObject().put(JsonData.ANSWER_INCIDENT_ID, id).put(JsonData.PARAM_UDID, Utils.getUdid(c))
                                                        .put(JsonData.PARAM_STATUS, status));
        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "Error updating incident", e);
            return null;
        }
    }

    public static Incident fromJSONObject(JSONObject obj) {
        try {
            json = obj;
            Log.d(Constants.PROJECT_TAG, obj.toString());
            Incident result = new Incident();
            result.latitude = obj.getDouble(JsonData.PARAM_INCIDENT_LATITUDE);
            result.longitude = obj.getDouble(JsonData.PARAM_INCIDENT_LONGITUDE);
            result.address = obj.getString(JsonData.PARAM_INCIDENT_ADDRESS);
            result.description = obj.getString(JsonData.PARAM_INCIDENT_DESCRIPTION);
            result.date = obj.getString(JsonData.PARAM_INCIDENT_DATE);
            result.state = obj.getString(JsonData.PARAM_INCIDENT_STATUS).charAt(0);
            result.categoryId = obj.getLong(JsonData.PARAM_INCIDENT_CATEGORY);

            result.confirms = obj.getInt(JsonData.PARAM_INCIDENT_CONFIRMS);
            result.invalidations = obj.getInt(JsonData.PARAM_INCIDENT_INVALIDATION);
            result.id = obj.getLong(JsonData.PARAM_INCIDENT_ID);
            result.pictures_far = obj.getJSONObject(JsonData.PARAM_INCIDENT_PICTURES).getJSONArray(JsonData.PARAM_INCIDENT_PICTURES_FAR);
            result.pictures_close = obj.getJSONObject(JsonData.PARAM_INCIDENT_PICTURES).getJSONArray(JsonData.PARAM_INCIDENT_PICTURES_CLOSE);
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
