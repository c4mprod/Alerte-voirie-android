package com.fabernovel.alertevoirie;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashScreenActivity extends Activity {

    protected static final long SPLASH_DURATION = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Thread() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                init();

                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime < SPLASH_DURATION) {
                    try {
                        sleep(SPLASH_DURATION - elapsedTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                next();
            }
        }.start();
    }

    private void init() {
        
    }

    private void next() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashScreenActivity.this, HomeActivity.class));
            }
        });
    }
}