package com.fabernovel.alertevoirie;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.fabernovel.alertevoirie.entities.IntentData;

public class AddCommentActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_add_comment);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_nouveau_rapport);

        final EditText commentField = (EditText) findViewById(R.id.EditText_comment);
        commentField.setText(getIntent().getStringExtra(IntentData.EXTRA_COMMENT));
        ((TextView) findViewById(R.id.TextView_remaining_chars)).setText((140 - commentField.getText().length()) + " car. restant");
        commentField.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                ((TextView) findViewById(R.id.TextView_remaining_chars)).setText((140 - commentField.getText().length()) + " car. restant");

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub

            }

        });

        // InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        // imm.showSoftInput(commentField, InputMethodManager.);

        // init button
        findViewById(R.id.Button_validate).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (commentField.getText().toString().trim().length() > 0) {
                    Intent result = new Intent();
                    result.putExtra(IntentData.EXTRA_COMMENT, commentField.getText().toString());
                    setResult(RESULT_OK, result);
                    finish();
                }else
                {
                    Toast.makeText(getApplicationContext(), "Veuillez entrer un commentaire", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
