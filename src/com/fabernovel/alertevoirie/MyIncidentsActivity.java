package com.fabernovel.alertevoirie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.utils.JSONAdapter;
import com.fabernovel.alertevoirie.utils.Utils;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;

public class MyIncidentsActivity extends ListActivity implements RequestListener {
    

    public static final String[] INCIDENTS       = new String[] { JsonData.PARAM_ONGOING_INCIDENTS, JsonData.PARAM_UPDATED_INCIDENTS,
            JsonData.PARAM_RESOLVED_INCIDENTS   };

    JSONArray[]                  data            = new JSONArray[3];

    RadioGroup                   tabs;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_report_lists);

        // launch request
        try {
            AVService.getInstance(this).postJSON(new JSONArray().put(new JSONObject().put(JsonData.PARAM_REQUEST, JsonData.VALUE_REQUEST_GET_MY_INCIDENTS)
                                                                                     .put(JsonData.PARAM_UDID, Utils.getUdid(this))), this);
            // showDialog(DIALOG_PROGRESS);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // get view references
        tabs = (RadioGroup) findViewById(R.id.RadioGroup_tabs);
        tabs.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(Constants.PROJECT_TAG, "checked : " + checkedId);
                setAdapterForTab(gettabIndex(checkedId));
            }
        });
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
                    for (int i = 0; i < data.length; i++) {
                        ((TextView) tabs.getChildAt(i)).setText(answer.getString(INCIDENTS[i]));
                        data[i] = answer.getJSONObject(JsonData.PARAM_INCIDENTS).getJSONArray(INCIDENTS[i]);
                        Log.d(Constants.PROJECT_TAG, "data : " + INCIDENTS[i] + " : " + data[i].length());

                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        setAdapterForTab(gettabIndex(tabs.getCheckedRadioButtonId()));

        // dismissDialog(DIALOG_PROGRESS);
    }

    private void setAdapterForTab(int tab) {
        // Log.d(Constants.PROJECT_TAG, "data " + data[tab].length());

        setListAdapter(new JSONAdapter(this, data[tab], R.layout.cell_report_noicon, new String[] { JsonData.PARAM_INCIDENT_DESCRIPTION,
                JsonData.PARAM_INCIDENT_ADDRESS }, new int[] { R.id.TextView_title, R.id.TextView_text }, JsonData.PARAM_INCIDENT_OBJECT) {
            @Override
            protected String getCategoryOfItem(int itemId) {
                return super.getCategoryOfItem(itemId).substring(0, 10);
            }
        });

    }
}
