package com.fabernovel.alertevoirie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;

import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.utils.JSONAdapter;
import com.fabernovel.alertevoirie.utils.Utils;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;

public class ExistingIncidentsActivity extends ListActivity implements RequestListener {
    private static final int DIALOG_PROGRESS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_existing_incidents);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_nouveau_rapport);

        try {
            JSONObject request = new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_GET_INCIDENTS_BY_POSITION)
                                                 .put(JsonData.PARAM_UDID, Utils.getUdid(this))
                                                 .put(JsonData.PARAM_RADIUS, JsonData.VALUE_RADIUS_CLOSE)
                                                 .put(JsonData.PARAM_POSITION,
                                                      new JSONObject().put(JsonData.PARAM_POSITION_LONGITUDE, "5.36628").put(JsonData.PARAM_POSITION_LATITUDE, "43.30957"));

            AVService.getInstance(this).postJSON(new JSONArray().put(request), this);

            showDialog(DIALOG_PROGRESS);

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // init button
        findViewById(R.id.Button_validate).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ExistingIncidentsActivity.this, SelectCategoryActivity.class));
            }
        });
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
                        AVService.getInstance(ExistingIncidentsActivity.this).cancelTask();
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
            super(context, data, cellLayout, from, to, jsonObjectName);
        }

        @Override
        protected String getCategoryOfItem(int itemId) {
            return super.getCategoryOfItem(itemId).substring(0, 10);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            if (getItemViewType(position) == TYPE_ITEM) {
                // TODO get incident photo
            }
            return v;
        }
    }
}
