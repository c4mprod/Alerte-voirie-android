package com.fabernovel.alertevoirie.entities;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.fabernovel.alertevoirie.utils.Utils;

public class Incident {
    public static final char STATUS_ONGOING = 'O';
    public static final char STATUS_RESOLVED = 'R';
    public static final char STATUS_UPDATED = 'U';
    
    public String address;
    public String description;
    public long   categoryId;
    public double latitude;
    public double longitude;
    public String date;
    public char state;
    public int    confirms;
    public long   id;

    public JSONObject getNewIncidentRequest(Context c) {
        try {
            return new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_NEW_INCIDENT)
                                   .put(JsonData.PARAM_UDID, Utils.getUdid(c))
                                   .put(JsonData.PARAM_INCIDENT,
                                        new JSONObject().put(JsonData.PARAM_INCIDENT_ADDRESS, address)
                                                        .put(JsonData.PARAM_INCIDENT_CATEGORY, categoryId)
                                                        .put(JsonData.PARAM_INCIDENT_DESCRIPTION, description))
                                   .put(JsonData.PARAM_POSITION,
                                        new JSONObject().put(JsonData.PARAM_POSITION_LATITUDE, latitude).put(JsonData.PARAM_POSITION_LONGITUDE, longitude));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Incident fromJSONObject(JSONObject obj) {
        try {
            Incident result = new Incident();
            result.address = obj.getString(JsonData.PARAM_INCIDENT_ADDRESS);
            result.description = obj.getString(JsonData.PARAM_INCIDENT_DESCRIPTION);
            result.date = obj.getString(JsonData.PARAM_INCIDENT_DATE);
            result.state = obj.getString(JsonData.PARAM_INCIDENT_STATUS).charAt(0);
            result.categoryId = obj.getLong(JsonData.PARAM_INCIDENT_CATEGORY);
            result.latitude = obj.getDouble(JsonData.PARAM_INCIDENT_LATITUDE);
            result.longitude = obj.getDouble(JsonData.PARAM_INCIDENT_LONGITUDE);
            result.confirms = obj.getInt(JsonData.PARAM_INCIDENT_CONFIRMS);
            result.id = obj.getLong(JsonData.PARAM_INCIDENT_ID);
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
