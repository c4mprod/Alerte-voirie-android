package com.fabernovel.alertevoirie;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.utils.JSONAdapter;
import com.fabernovel.alertevoirie.utils.Utils;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;

public class MyIncidentsActivity extends ListActivity implements RequestListener {

    public static final String[] INCIDENTS       = new String[] { JsonData.PARAM_DECLARED_INCIDENTS, JsonData.PARAM_UPDATED_INCIDENTS,
            JsonData.PARAM_RESOLVED_INCIDENTS   };
    private static final int     DIALOG_PROGRESS = 123456789;
    JSONObject                   data;
    String[]                     title           = new String[3];
    ToggleButton                 tbmap;
    RadioGroup                   tabs;
    protected int                checked;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_report_lists);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_mes_rapports);
        tabs = (RadioGroup) findViewById(R.id.RadioGroup_tabs);
        tbmap = (ToggleButton) findViewById(R.id.ToggleButton01);
        tbmap.setChecked(false);
        tbmap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tbmap.setChecked(false);
                    Intent i = new Intent(MyIncidentsActivity.this, MyIncidentsActivityMap.class);
                    i.putExtra("tab1", title[0]);
                    i.putExtra("tab2", title[1]);
                    i.putExtra("tab3", title[2]);
                    i.putExtra("datas", data.toString());
                    i.putExtra("tab", checked);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(i);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }

            }
        });

        if (getIntent().getExtras() != null) {

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

            setAdapterForTab(gettabIndex(tabs.getCheckedRadioButtonId()));

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
                setAdapterForTab(gettabIndex(checkedId));
            }
        });

        tabs.check(getId());
    }

    private int gettabIndex(int id) {
        switch (id) {
            case R.id.Tab_ongoing:
                return 0;
            case R.id.Tab_updated:
                return 1;
            case R.id.Tab_resolved:
                return 2;

            default:
                return -1;
        }
    }

    @Override
    public void onRequestcompleted(int requestCode, Object result) {
        Log.d(Constants.PROJECT_TAG, "result = " + result);
        if (requestCode == AVService.REQUEST_JSON && result != null) {
            try {
                JSONObject answer = new JSONArray((String) result).getJSONObject(0);

                Log.d(Constants.PROJECT_TAG, "answer = " + answer);

                /*
                 * 10-01 18:25:27.116: DEBUG/Alerte Voirie(24267): result =
                 * [{"answer":
                 * {"status": 0,
                 * "incidents": {
                 * "updated_incidents": [],
                 * "resolved_incidents": [],
                 * "ongoing_incidents": [{"incidentObj": {"descriptive": "Bloc a l'envers !!", "pictures": {"far": [], "close": []}, "categoryId": 24, "date":
                 * "2010-10-01 11:59:47.998833", "state": "O", "address": "Boulevard de Dunkerque", "lat": 43.306460000000001, "lng": 5.3673489999999999,
                 * "confirms": 0, "id": 38}}, {"incidentObj": {"descriptive": "Bloc ? qui tra\ufffd par terre.", "pictures": {"far": [], "close": []},
                 * "categoryId": 10, "date": "2010-10-01 12:09:17.580111", "state": "O", "address": "32 Quai du Lazaret", "lat": 43.309669999999997, "lng":
                 * 5.3672469999999999, "confirms": 0, "id": 39}}]
                 * },
                 * "updated_incidents": 0,
                 * "ongoing_incidents": 2,
                 * "resolved_incidents": 0},
                 * "request": "getReports"}]
                 */

                if (JsonData.VALUE_REQUEST_GET_MY_INCIDENTS.equals(answer.getString(JsonData.PARAM_REQUEST))) {
                    answer = answer.getJSONObject(JsonData.PARAM_ANSWER);
                    for (int i = 0; i < 3; i++) {
                        title[i] = answer.getString(INCIDENTS[i])
                                   + "\n"
                                   + (INCIDENTS[i].equals("declared_incidents") ? getString(R.string.home_label_current)
                                                                               : INCIDENTS[i].equals("updated_incidents") ? getString(R.string.home_label_update)
                                                                                                                         : INCIDENTS[i].equals("resolved_incidents") ? getString(R.string.home_label_solved)
                                                                                                                                                                    : "");
                        ((TextView) tabs.getChildAt(i)).setText(title[i]);
                        if (title[i].startsWith("0")) ((TextView) tabs.getChildAt(i)).setEnabled(false);

                    }
                    data = answer.getJSONObject(JsonData.PARAM_INCIDENTS);
                    // Log.d(Constants.PROJECT_TAG, "data : " + INCIDENTS[i] + " : " + data.getJSONArray(INCIDENTS[i]).length());

                }
            } catch (JSONException e) {
                Log.e(Constants.PROJECT_TAG, "JsonException in onRequestCompleted", e);
            } finally {
                dismissDialog(DIALOG_PROGRESS);
            }
        }

        setAdapterForTab(gettabIndex(tabs.getCheckedRadioButtonId()));

        // dismissDialog(DIALOG_PROGRESS);
    }

    private int getId() {
        switch (checked) {
            case R.id.Tab_Map_ongoing:
                checked = R.id.Tab_ongoing;

                break;
            case R.id.Tab_Map_resolved:
                checked = R.id.Tab_resolved;

                break;
            case R.id.Tab_Map_updated:
                checked = R.id.Tab_updated;

                break;

            default:
                checked = R.id.Tab_ongoing;
                break;
        }
        return checked;
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
                        AVService.getInstance(MyIncidentsActivity.this).cancelTask();
                        finish();
                    }
                });
                pd.setMessage(getString(R.string.ui_message_loading));
                return pd;

            default:
                return super.onCreateDialog(id);
        }
    }

    private void setAdapterForTab(int tab) {

        try {
            Log.d(Constants.PROJECT_TAG, data.getJSONArray(INCIDENTS[tab]).toString());

            setListAdapter(new JSONAdapter(this, data.getJSONArray(INCIDENTS[tab]), R.layout.cell_report_noicon, new String[] {
                    JsonData.PARAM_INCIDENT_DESCRIPTION, JsonData.PARAM_INCIDENT_ADDRESS }, new int[] { R.id.TextView_title, R.id.TextView_text }, null,
                                           JsonData.PARAM_INCIDENT_DATE, R.layout.cell_category) {
                @Override
                protected String getCategoryOfItem(int itemId) {
                    String date = super.getCategoryOfItem(itemId).substring(0, 10);

                    Log.d(Constants.PROJECT_TAG, date);

                    return ((String) DateFormat.format("MMMM yyyy",
                                                       new Date(Integer.parseInt(date.substring(0, 4)), Integer.parseInt(date.substring(5, 7)) - 1,
                                                                Integer.parseInt(date.substring(8, 10))))).replace("39", "20");
                    // return date;
                }
            });
        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "JSONException in setAdapterForTab", e);
        }

    }
}
