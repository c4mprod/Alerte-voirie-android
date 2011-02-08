package com.fabernovel.alertevoirie;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;

import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.Incident;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.entities.Last_Location;
import com.fabernovel.alertevoirie.utils.JSONAdapter;
import com.fabernovel.alertevoirie.utils.Utils;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;

public class NewsActivity extends ListActivity implements RequestListener {
    private static final int          DIALOG_PROGRESS = 0;
    private JSONObject                response;
    private TreeMap<Long, JSONObject> logs;
    private ArrayList<JSONObject>     logList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_report_lists);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_autour_de_vous);

        findViewById(R.id.RadioGroup_tabs).setVisibility(View.GONE);
        findViewById(R.id.ToggleButton01).setVisibility(View.GONE);

        try {
            JSONObject request = new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_GET_USERS_ACTVITIES)
            // .put(JsonData.PARAM_UDID, Utils.getUdid(this))
            // .put(JsonData.PARAM_RADIUS, JsonData.VALUE_RADIUS_FAR)
                                                 .put(JsonData.PARAM_POSITION,
                                                      new JSONObject().put(JsonData.PARAM_POSITION_LONGITUDE, Last_Location.longitude)
                                                                      .put(JsonData.PARAM_POSITION_LATITUDE, Last_Location.latitude));

            AVService.getInstance(this).postJSON(new JSONArray().put(request), this);

            showDialog(DIALOG_PROGRESS);

        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "error in onCreate : JSONException", e);
        }

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
                        removeDialog(DIALOG_PROGRESS);
                    }
                });
                pd.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        AVService.getInstance(NewsActivity.this).cancelTask();
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
            response = responses.getJSONObject(0);
            TreeMap<Long, JSONObject> events = new TreeMap<Long, JSONObject>();

            if (requestCode == AVService.REQUEST_JSON) {

                logs = new TreeMap<Long, JSONObject>();
                logList = new ArrayList<JSONObject>();
                for (int i = 0; i < response.getJSONObject(JsonData.PARAM_ANSWER).getJSONArray(JsonData.PARAM_INCIDENT_LOG).length(); i++) {
                    JSONObject job = response.getJSONObject(JsonData.PARAM_ANSWER).getJSONArray(JsonData.PARAM_INCIDENT_LOG).getJSONObject(i);
                    logs.put(job.getLong(JsonData.ANSWER_INCIDENT_ID), job);
                    logList.add(job);
                }

                if (JsonData.VALUE_REQUEST_GET_USERS_ACTVITIES.equals(response.getString(JsonData.PARAM_REQUEST))) {

                    JSONArray items = new JSONArray();

                    for (int i = 0; i < response.getJSONObject(JsonData.PARAM_ANSWER).getJSONObject(JsonData.PARAM_INCIDENTS)
                                                .getJSONArray(JsonData.PARAM_UPDATED_INCIDENTS).length(); i++) {
                        JSONObject job = response.getJSONObject(JsonData.PARAM_ANSWER).getJSONObject(JsonData.PARAM_INCIDENTS)
                                                 .getJSONArray(JsonData.PARAM_ONGOING_INCIDENTS).getJSONObject(i);
                        if (logs.containsKey(job.getLong(JsonData.PARAM_INCIDENT_ID))) events.put(job.getLong(JsonData.PARAM_INCIDENT_ID), job);// items.put(job);
                    }

                    for (int i = 0; i < response.getJSONObject(JsonData.PARAM_ANSWER).getJSONObject(JsonData.PARAM_INCIDENTS)
                                                .getJSONArray(JsonData.PARAM_UPDATED_INCIDENTS).length(); i++) {
                        JSONObject job = response.getJSONObject(JsonData.PARAM_ANSWER).getJSONObject(JsonData.PARAM_INCIDENTS)
                                                 .getJSONArray(JsonData.PARAM_UPDATED_INCIDENTS).getJSONObject(i);
                        if (logs.containsKey(job.getLong(JsonData.PARAM_INCIDENT_ID))) events.put(job.getLong(JsonData.PARAM_INCIDENT_ID), job);
                    }

                    for (int i = 0; i < response.getJSONObject(JsonData.PARAM_ANSWER).getJSONObject(JsonData.PARAM_INCIDENTS)
                                                .getJSONArray(JsonData.PARAM_RESOLVED_INCIDENTS).length(); i++) {
                        JSONObject job = response.getJSONObject(JsonData.PARAM_ANSWER).getJSONObject(JsonData.PARAM_INCIDENTS)
                                                 .getJSONArray(JsonData.PARAM_RESOLVED_INCIDENTS).getJSONObject(i);
                        if (logs.containsKey(job.getLong(JsonData.PARAM_INCIDENT_ID))) events.put(job.getLong(JsonData.PARAM_INCIDENT_ID), job);
                    }

                    for (JSONObject log : logList) {
                        JSONObject job = new JSONObject(events.get(log.getLong(JsonData.ANSWER_INCIDENT_ID)).toString());
                        job.put(JsonData.PARAM_INCIDENT_DATE, log.getString(JsonData.PARAM_INCIDENT_DATE));
                        items.put(job);
                    }

                    setListAdapter(new MagicAdapter(this, items, R.layout.cell_report, new String[] { JsonData.PARAM_INCIDENT_DESCRIPTION,
                            JsonData.PARAM_INCIDENT_ADDRESS }, new int[] { R.id.TextView_title, R.id.TextView_text }, null));
                }
            }

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        dismissDialog(DIALOG_PROGRESS);
    }

    class MagicAdapter extends JSONAdapter {
        public MagicAdapter(Context context, JSONArray data, int cellLayout, String[] from, int[] to, String jsonObjectName) {
            super(context, data, cellLayout, from, to, jsonObjectName, JsonData.PARAM_INCIDENT_DATE, R.layout.cell_category);
        }

        @Override
        protected String getCategoryOfItem(int itemId) {
            String date = super.getCategoryOfItem(itemId).substring(0, 10);

            Log.d(Constants.PROJECT_TAG, date);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            new Date(10, 10, 10);

            try {
                return ((String) DateFormat.format("d MMMM yyyy", sdf.parse(date)));
            } catch (ParseException e) {
                Log.e(Constants.PROJECT_TAG, "Error parsing date", e);
            }
            return date;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            if (getItemViewType(position) == TYPE_ITEM) {
                ImageView iw = (ImageView) v.findViewById(R.id.ImageView_icn);
                iw.setVisibility(View.VISIBLE);
                JSONObject item = (JSONObject) getItem(position);
                Incident incident = Incident.fromJSONObject(item);

                String status = null;
                try {
                    JSONObject job = logList.get(position);
                    if (Constants.DEBUGMODE) Log.d(Constants.PROJECT_TAG, "getView : incident id " + incident.id);
                    if (job != null) {
                        status = job.getString(JsonData.PARAM_STATUS);
                    }

                } catch (JSONException e) {
                    Log.e(Constants.PROJECT_TAG, "JSONException in getView", e);
                    switch (incident.state) {
                        case 'O':
                            iw.setImageResource(R.drawable.icn_creer);
                            break;
                        case 'R':
                            iw.setImageResource(R.drawable.icn_incident_resolu2);
                            break;
                        case 'U':
                            iw.setImageResource(R.drawable.icn_photo_ajoutee);
                            break;

                        default:
                            break;

                    }

                    if (incident.confirms > 0) {
                        iw.setImageResource(R.drawable.icn_incident_confirme);
                    }

                    if (incident.invalidations > 0) {
                        iw.setImageResource(R.drawable.icn_incident_nonvalide);
                    }

                }

                if (JsonData.PARAM_UPDATE_INCIDENT_CONFIRMED.equals(status)) {
                    iw.setImageResource(R.drawable.icn_incident_confirme);
                } else if (JsonData.PARAM_UPDATE_INCIDENT_INVALID.equals(status)) {
                    iw.setImageResource(R.drawable.icn_incident_nonvalide);
                } else if (JsonData.PARAM_UPDATE_INCIDENT_RESOLVED.equals(status)) {
                    iw.setImageResource(R.drawable.icn_incident_resolu2);
                } else if (JsonData.PARAM_UPDATE_INCIDENT_NEW.equals(status)) {
                    iw.setImageResource(R.drawable.icn_creer);
                } else if (JsonData.PARAM_UPDATE_INCIDENT_PHOTO.equals(status)) {
                    iw.setImageResource(R.drawable.icn_photo_ajoutee);
                } else {
                    iw.setVisibility(View.INVISIBLE);
                }

                ((ImageView) v.findViewById(R.id.ImageView_icon)).setVisibility(View.GONE);

                ((ImageView) v.findViewById(R.id.ImageView_icon)).setImageBitmap(null);
            }
            return v;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Log.d(Constants.PROJECT_TAG, "onListItemClick : "+l.getAdapter().getItem(position));
        Intent i = new Intent(this, ReportDetailsActivity.class);
        i.putExtra("existing", true);
        i.putExtra("event", l.getAdapter().getItem(position).toString());

        try {
            Incident incident = Incident.fromJSONObject(new JSONObject(l.getAdapter().getItem(position).toString()));
            JSONObject job = logs.get(incident.id);
            if (job != null) {
                if (JsonData.PARAM_UPDATE_INCIDENT_INVALID.equals(job.getString(JsonData.PARAM_STATUS))
                    || JsonData.PARAM_UPDATE_INCIDENT_RESOLVED.equals(job.getString(JsonData.PARAM_STATUS))) return;

            }

        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "JSONException in onListItemClick", e);
        }
        startActivity(i);
        // super.onListItemClick(l, v, position, id);
    }
}
