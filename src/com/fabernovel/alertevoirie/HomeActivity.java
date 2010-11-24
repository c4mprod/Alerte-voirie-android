package com.fabernovel.alertevoirie;

import com.fabernovel.alertevoirie.entities.Constants;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class HomeActivity extends Activity implements OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_home);

        // init buttons
        findViewById(R.id.Button_news).setOnClickListener(this);
        findViewById(R.id.Button_reports).setOnClickListener(this);
        findViewById(R.id.Button_new_incident).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Button_news:
                startActivity(new Intent(this, NewsActivity.class));
//                startActivity(new Intent(this, ExistingIncidentsActivity.class));
                break;
            case R.id.Button_reports:
                startActivity(new Intent(this, MyIncidentsActivity.class));
                break;
            case R.id.Button_new_incident:
                Intent i = new Intent(this, ExistingIncidentsActivity.class);
                i.putExtra(Constants.NEW_REPORT, true);
                startActivity(i);
                //startActivity(new Intent(this, ReportDetailsActivity.class));
                break;

            default:
                break;
        }
    }
}
