package com.fabernovel.alertevoirie;

import com.fabernovel.alertevoirie.entities.IntentData;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

public class AddCommentActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_add_comment);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_nouveau_rapport);
        
        final EditText commentField = (EditText) findViewById(R.id.EditText_comment);
        commentField.setText(getIntent().getStringExtra(IntentData.EXTRA_COMMENT));
        
//        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
//        imm.showSoftInput(commentField, InputMethodManager.);
        
        //init button
        findViewById(R.id.Button_validate).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent result = new Intent();
                result.putExtra(IntentData.EXTRA_COMMENT, commentField .getText().toString());
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }
}
