/**
 * This file is part of the Alerte Voirie project.
 * 
 * Copyright (C) 2010-2011 C4M PROD
 * 
 * Alerte Voirie is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alerte Voirie is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Alerte Voirie.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.fabernovel.alertevoirie;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.TreeMap;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.Incident;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.entities.Last_Location;
import com.fabernovel.alertevoirie.utils.JSONAdapter;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;

public class NewsActivity extends ListActivity implements RequestListener {
    private static final int            DIALOG_PROGRESS = 0;
    private JSONObject                  response;
    private SparseArray<JSONObject>     logs;
    private TreeMap<String, JSONObject> logList;
    private Vector<Integer>             lock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_report_lists);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_autour_de_vous);

        findViewById(R.id.RadioGroup_tabs).setVisibility(View.GONE);
        findViewById(R.id.ToggleButton01).setVisibility(View.GONE);
    }

    private void sendRequest() {
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
    protected void onStart() {
        super.onStart();
        sendRequest();
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
        
*/
//@formatter:on 

        try {
            JSONArray responses;
            responses = new JSONArray((String) result);
            response = responses.getJSONObject(0);
            SparseArray<JSONObject> events = new SparseArray<JSONObject>();

            if (requestCode == AVService.REQUEST_JSON) {

                if (JsonData.VALUE_REQUEST_GET_USERS_ACTVITIES.equals(response.getString(JsonData.PARAM_REQUEST))) {
                    lock = new Vector<Integer>();
                    logs = new SparseArray<JSONObject>();
                    logList = new TreeMap<String, JSONObject>(Collections.reverseOrder());
                    JSONArray incidentLog = response.getJSONObject(JsonData.PARAM_ANSWER).getJSONArray(JsonData.PARAM_INCIDENT_LOG);
                    for (int i = 0; i < incidentLog.length(); i++) {
                        JSONObject job = incidentLog.getJSONObject(i);
                        logs.put(job.getInt(JsonData.ANSWER_INCIDENT_ID), job);
                        logList.put(job.getString(JsonData.PARAM_INCIDENT_DATE) + job.getLong(JsonData.ANSWER_INCIDENT_ID), job);
                    }

                    JSONArray items = new JSONArray();

                    JSONArray ongoingIncidents = response.getJSONObject(JsonData.PARAM_ANSWER)
                                                         .getJSONObject(JsonData.PARAM_INCIDENTS)
                                                         .getJSONArray(JsonData.PARAM_ONGOING_INCIDENTS);
                    for (int i = 0; i < ongoingIncidents.length(); i++) {
                        JSONObject job = ongoingIncidents.getJSONObject(i);
                        int key = job.getInt(JsonData.PARAM_INCIDENT_ID);
                        if (logs.get(key) != null) {
                            Log.d("AlerteVoirie_PM", "add ongoing incident "+key);
                            events.put(key, job);// items.put(job);
                        }
                    }

                    JSONArray updatedIncidents = response.getJSONObject(JsonData.PARAM_ANSWER)
                                                         .getJSONObject(JsonData.PARAM_INCIDENTS)
                                                         .getJSONArray(JsonData.PARAM_UPDATED_INCIDENTS);
                    for (int i = 0; i < updatedIncidents.length(); i++) {
                        JSONObject job = updatedIncidents.getJSONObject(i);
                        int key = job.getInt(JsonData.PARAM_INCIDENT_ID);
                        if (logs.get(key) != null) {
                            Log.d("AlerteVoirie_PM", "add updated incident "+key);
                            events.put(key, job);
                        }
                    }

                    JSONArray resolvedIncidents = response.getJSONObject(JsonData.PARAM_ANSWER)
                                                          .getJSONObject(JsonData.PARAM_INCIDENTS)
                                                          .getJSONArray(JsonData.PARAM_RESOLVED_INCIDENTS);
                    for (int i = 0; i < resolvedIncidents.length(); i++) {
                        JSONObject job = resolvedIncidents.getJSONObject(i);
                        int key = job.getInt(JsonData.PARAM_INCIDENT_ID);
                        if (logs.get(key) != null) {
                            Log.d("AlerteVoirie_PM", "add resolved incident "+key);
                            events.put(key, job);
                        }
                    }

                    for (JSONObject log : logList.values()) {
                        int id = log.getInt(JsonData.ANSWER_INCIDENT_ID);
                        Log.d("AlerteVoirie_PM", "key bug "+id);
                        JSONObject jsonObject = events.get(id);
                        if (jsonObject != null) {                            
                            String json = jsonObject.toString();
                            JSONObject job = new JSONObject(json);
                            job.put(JsonData.PARAM_INCIDENT_DATE, log.getString(JsonData.PARAM_INCIDENT_DATE));
                            items.put(job);
                            
                            if (JsonData.PARAM_UPDATE_INCIDENT_INVALID.equals(log.getString(JsonData.PARAM_STATUS))
                                    || JsonData.PARAM_UPDATE_INCIDENT_RESOLVED.equals(log.getString(JsonData.PARAM_STATUS))) {
                                lock.add(id);
                            }
                        }
                    }

                    setListAdapter(new MagicAdapter(this, items, R.layout.cell_report, new String[] { JsonData.PARAM_INCIDENT_DESCRIPTION,
                            JsonData.PARAM_INCIDENT_ADDRESS }, new int[] { R.id.TextView_title, R.id.TextView_text }, null));
                }

            }

        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "error in onRequestcompleted : ", e);
        } catch (ClassCastException e) {
            Log.e(Constants.PROJECT_TAG, "error in onRequestcompleted : CLasscastException", e);
        } catch (NullPointerException e) {
            Log.e(Constants.PROJECT_TAG, "error in onRequestcompleted : NullPointerException", e);
        } finally {

            if (requestCode == AVService.REQUEST_JSON) dismissDialog(DIALOG_PROGRESS);
        }

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
        public boolean isEnabled(int position) {
            JSONObject item = (JSONObject) getItem(position);
            
            //detect if invalid
            try {
                if (item.getInt(JsonData.PARAM_INCIDENT_INVALIDATION)>0 || "R".equals(item.getString(JsonData.PARAM_INCIDENT_STATUS))) {
                    return false;
                }
            } catch (JSONException e1) {
            }
            
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            if (getItemViewType(position) == TYPE_ITEM) {
                ImageView iw = (ImageView) v.findViewById(R.id.ImageView_icn);
                iw.setVisibility(View.VISIBLE);
                JSONObject item = (JSONObject) getItem(position);
                Incident incident = Incident.fromJSONObject(getApplicationContext(), item);

                String status = null;
                try {
                    JSONObject job = (JSONObject) logList.values().toArray()[getRealPositionOfItem(position)];// -getRealPositionOfItem(position));
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
                    ((TextView) v.findViewById(R.id.TextView_title)).setText(getString(R.string.news_incidents_confirmed) + incident.description);
                } else if (JsonData.PARAM_UPDATE_INCIDENT_INVALID.equals(status)) {
                    iw.setImageResource(R.drawable.icn_incident_nonvalide);
                    ((TextView) v.findViewById(R.id.TextView_title)).setText(getString(R.string.news_incidents_invalidated) + incident.description);
                } else if (JsonData.PARAM_UPDATE_INCIDENT_RESOLVED.equals(status)) {
                    iw.setImageResource(R.drawable.icn_incident_resolu2);
                    ((TextView) v.findViewById(R.id.TextView_title)).setText(getString(R.string.news_incidents_resolved) + incident.description);
                } else if (JsonData.PARAM_UPDATE_INCIDENT_NEW.equals(status)) {
                    iw.setImageResource(R.drawable.icn_creer);
                    ((TextView) v.findViewById(R.id.TextView_title)).setText(getString(R.string.news_incidents_created) + incident.description);
                } else if (JsonData.PARAM_UPDATE_INCIDENT_PHOTO.equals(status)) {
                    iw.setImageResource(R.drawable.icn_photo_ajoutee);
                    ((TextView) v.findViewById(R.id.TextView_title)).setText(getString(R.string.news_incidents_picture_added) + incident.description);
                } else {
                    iw.setVisibility(View.INVISIBLE);
                }

                if (lock.contains(incident.id) || !isEnabled(position)) {
                    v.findViewById(R.id.Arrow_details).setVisibility(View.GONE);
                } else {
                    v.findViewById(R.id.Arrow_details).setVisibility(View.VISIBLE);
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

        JSONObject item = (JSONObject) l.getAdapter().getItem(position);
        i.putExtra("event", item.toString());

        try {
            Incident incident = Incident.fromJSONObject(this, new JSONObject(((MagicAdapter) getListAdapter()).getItem(position).toString()));

            if (lock.contains(incident.id)) return;
            /*
             * JSONObject job = (JSONObject) logList.values().toArray()[((MagicAdapter) getListAdapter()).getRealPositionOfItem(position)];
             * if (job != null) {
             * if (JsonData.PARAM_UPDATE_INCIDENT_INVALID.equals(job.getString(JsonData.PARAM_STATUS))
             * || JsonData.PARAM_UPDATE_INCIDENT_RESOLVED.equals(job.getString(JsonData.PARAM_STATUS))) return;
             * } else if (incident.state == 'R') return;
             */

        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "JSONException in onListItemClick", e);
        }
        startActivityForResult(i, 1);
        // startActivity(i);
        // super.onListItemClick(l, v, position, id);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.report_detail_new_report_ok).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    NewsActivity.this.finish();
                    // Toast.makeText(this, R.string.report_detail_new_report_ok, Toast.LENGTH_SHORT).show();
                    NewsActivity.this.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
            AlertDialog alert = builder.create();
            alert.show();

        }
    }
}
