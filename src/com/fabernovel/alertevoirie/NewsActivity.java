package com.fabernovel.alertevoirie;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.utils.JSONAdapter;
import com.fabernovel.alertevoirie.utils.Utils;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;

public class NewsActivity extends ListActivity implements RequestListener {
    private static final int DIALOG_PROGRESS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_report_lists);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_autour_de_vous);

        try {
            JSONObject request = new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_GET_INCIDENTS_BY_POSITION)
                                                 .put(JsonData.PARAM_UDID, Utils.getUdid(this))
                                                 .put(JsonData.PARAM_RADIUS, JsonData.VALUE_RADIUS_FAR)
                                                 .put(JsonData.PARAM_POSITION,
                                                      new JSONObject().put(JsonData.PARAM_POSITION_LONGITUDE, "5.36628").put(JsonData.PARAM_POSITION_LATITUDE,
                                                                                                                             "43.30957"));

            AVService.getInstance(this).postJSON(new JSONArray().put(request), this);

            showDialog(DIALOG_PROGRESS);

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            JSONObject response = responses.getJSONObject(0);

            if (requestCode == AVService.REQUEST_JSON) {
                if (JsonData.VALUE_REQUEST_GET_INCIDENTS_BY_POSITION.equals(response.getString(JsonData.PARAM_REQUEST))) {
                    JSONArray items = response.getJSONObject(JsonData.PARAM_ANSWER).getJSONArray(JsonData.PARAM_CLOSEST_INCIDENTS);
                    setListAdapter(new MagicAdapter(this, items, R.layout.cell_report, new String[] { JsonData.PARAM_INCIDENT_DESCRIPTION,
                            JsonData.PARAM_INCIDENT_ADDRESS }, new int[] { R.id.TextView_title, R.id.TextView_text }, JsonData.PARAM_INCIDENT_OBJECT));
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
            
            new Date(10,10,10);

            try {
                return ((String) DateFormat.format("MMMM yyyy", sdf.parse(date)));
            } catch (ParseException e) {
                Log.e(Constants.PROJECT_TAG, "Error parsing date", e);
            }
            return date;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            if (getItemViewType(position) == TYPE_ITEM) {
                ImageView iw = (ImageView) v.findViewById(R.id.ImageView_icon);
                JSONObject item = (JSONObject) getItem(position);
                try {
                    switch (item.getString(JsonData.PARAM_INCIDENT_STATUS).charAt(0)) {
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
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return v;
        }
    }
}
