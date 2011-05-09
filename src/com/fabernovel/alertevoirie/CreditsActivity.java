package com.fabernovel.alertevoirie;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class CreditsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_credits);

        findViewById(R.id.button_contact).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mail = new Intent(Intent.ACTION_SENDTO);
                mail.setData(Uri.parse(String.format("mailto:%s", getString(R.string.credits_email))));
                startActivity(mail);
            }
        });
    }
}
