package com.fabernovel.alertevoirie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.fabernovel.alertevoirie.entities.Category;
import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.Incident;
import com.fabernovel.alertevoirie.entities.IntentData;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;

public class ReportDetailsActivity extends Activity implements OnClickListener, RequestListener {
    private static final String EMPTY_STRING     = "";

    private static final int    REQUEST_CATEGORY = 0;
    private static final int    REQUEST_POSITION = 1;
    private static final int    REQUEST_COMMENT  = 2;

    private static final int    DIALOG_PROGRESS  = 0;

    private Uri                 uriOfPicFromCamera;

    private Incident            currentIncident  = new Incident();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_report_details);

        // init title
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_nouveau_rapport);
        getWindow().setTitle(getString(R.string.report_detail_new_report_title));

        // init buttons
        findViewById(R.id.ImageView_far).setOnClickListener(this);
        findViewById(R.id.ImageView_close).setOnClickListener(this);
        findViewById(R.id.LinearLayout_category).setOnClickListener(this);
        findViewById(R.id.LinearLayout_where).setOnClickListener(this);
        findViewById(R.id.LinearLayout_comment).setOnClickListener(this);
        findViewById(R.id.Button_validate).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ImageView_far:
            case R.id.ImageView_close:
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File tmpFile = null;
                try {
                    tmpFile = File.createTempFile("capture", "tmp");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                uriOfPicFromCamera = Uri.fromFile(tmpFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uriOfPicFromCamera);

                startActivityForResult(intent, v.getId());
                break;

            case R.id.LinearLayout_category:
                startActivityForResult(new Intent(this, SelectCategoryActivity.class), REQUEST_CATEGORY);
                break;
            case R.id.LinearLayout_where:
                startActivityForResult(new Intent(this, SelectPositionActivity.class), REQUEST_POSITION);
                break;
            case R.id.LinearLayout_comment:
                Intent intent2 = new Intent(this, AddCommentActivity.class);
                intent2.putExtra(IntentData.EXTRA_COMMENT, ((TextView) findViewById(R.id.TextView_comment)).getText().toString());
                startActivityForResult(intent2, REQUEST_COMMENT);
                break;

            case R.id.Button_validate:
                postNewIncident();
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case R.id.ImageView_far:
            case R.id.ImageView_close:
                if (resultCode == RESULT_OK) {
                    try {
                        InputStream in;
                        BitmapFactory.Options opt = new BitmapFactory.Options();

                        // get the sample size to have a smaller image
                        in = getContentResolver().openInputStream(uriOfPicFromCamera);
                        opt.inSampleSize = getSampleSize(getContentResolver().openInputStream(uriOfPicFromCamera));
                        in.close();

                        // decode a sampled version of the picture
                        in = getContentResolver().openInputStream(uriOfPicFromCamera);
                        Bitmap picture = BitmapFactory.decodeStream(in, null, opt);
                        in.close();

                        // save the new image
                        String pictureName = requestCode == R.id.ImageView_far ? "capture_far" : "capture_close";
                        FileOutputStream fos = openFileOutput(pictureName, MODE_PRIVATE);
                        picture.compress(CompressFormat.PNG, 0, fos);
                        fos.close();

                        setPictureToImageView(pictureName, (ImageView) findViewById(requestCode));
                        // FileOutputStream fos = openFileOutput("capture", MODE_WORLD_READABLE);
                        // InputStream in = getContentResolver().openInputStream(uriOfPicFromCamera);
                        // Utils.fromInputToOutput(in, fos);
                        // fos.close();
                        // in.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (resultCode == RESULT_CANCELED) {
                    if (uriOfPicFromCamera != null) {
                        File tmpFile = new File(uriOfPicFromCamera.getPath());
                        tmpFile.delete();
                        uriOfPicFromCamera = null;
                    }
                }
                break;

            case REQUEST_CATEGORY:
                if (resultCode == RESULT_OK) {
                    String subCategories = EMPTY_STRING, category = EMPTY_STRING;
                    long catId = data.getLongExtra(IntentData.EXTRA_CATEGORY_ID, 0);
                    currentIncident.categoryId = catId;

                    do {
                        Cursor c = getContentResolver().query(ContentUris.withAppendedId(Category.CONTENT_URI, catId),
                                                              new String[] { Category.PARENT, Category.NAME }, null, null, null);

                        if (c.moveToFirst()) {
                            catId = c.getInt(c.getColumnIndex(Category.PARENT));
                            if (catId != 0) {
                                subCategories = c.getString(c.getColumnIndex(Category.NAME))
                                                + (subCategories.length() > 0 ? ", " + subCategories : EMPTY_STRING);
                            } else {
                                category = c.getString(c.getColumnIndex(Category.NAME));
                            }
                        }
                        c.close();
                    } while (catId > 0);

                    ((TextView) findViewById(R.id.TextView_sub_categories)).setText(subCategories);
                    ((TextView) findViewById(R.id.TextView_main_category)).setText(category);

                }
                break;
            case REQUEST_POSITION:
                if (resultCode == RESULT_OK) {
                    currentIncident.address = data.getStringExtra(IntentData.EXTRA_ADDRESS);
                    currentIncident.longitude = data.getDoubleExtra(IntentData.EXTRA_LONGITUDE, 0);
                    currentIncident.latitude = data.getDoubleExtra(IntentData.EXTRA_LATITUDE, 0);
                    ((TextView) findViewById(R.id.TextView_address)).setText(currentIncident.address);
                }
                break;
            case REQUEST_COMMENT:
                if (resultCode == RESULT_OK) {
                    currentIncident.description = data.getStringExtra(IntentData.EXTRA_COMMENT);
                    ((TextView) findViewById(R.id.TextView_comment)).setText(currentIncident.description);
                }

            default:
                break;
        }
    }

    private void setPictureToImageView(String pictureName, ImageView imageView) {
        Bitmap picture = null;
        try {
            InputStream in = openFileInput(pictureName);
            picture = BitmapFactory.decodeStream(in);
            in.close();

            LayerDrawable d = (LayerDrawable) getResources().getDrawable(R.drawable.editable_picture_frame);
            if (picture.getHeight() > picture.getWidth()) {
                Matrix m = new Matrix();
                m.postRotate(90);
                picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), m, true);
            }
            picture = Bitmap.createScaledBitmap(picture, d.getIntrinsicWidth(), d.getIntrinsicHeight(), true);

            d.setDrawableByLayerId(R.id.picture, new BitmapDrawable(picture));
            imageView.setImageDrawable(d);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getSampleSize(InputStream in) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, opt);
        int dimension;
        if (opt.outWidth > opt.outHeight) {
            dimension = opt.outWidth;
        } else {
            dimension = opt.outHeight;
        }

        return (int) dimension / Constants.PICTURE_PREFERED_WIDTH;
    }

    private void postNewIncident() {
        JSONObject newIncidentRequest = currentIncident.getNewIncidentRequest(this);

        if (newIncidentRequest != null) {
            AVService.getInstance(this).postJSON(new JSONArray().put(newIncidentRequest), this);
            showDialog(DIALOG_PROGRESS);
        } else {
            // TODO handle event
        }
    }

    @Override
    public void onRequestcompleted(int requestCode, Object result) {
        if (requestCode == AVService.REQUEST_JSON && result != null) {
            try {
                JSONObject answer = new JSONArray((String) result).getJSONObject(0);
                if (JsonData.VALUE_REQUEST_NEW_INCIDENT.equals(answer.getString(JsonData.PARAM_REQUEST))
                    && JsonData.VALUE_INCIDENT_SAVED.equals(answer.getJSONObject(JsonData.PARAM_ANSWER).get(JsonData.PARAM_STATUS))) {
                
                    //TODO do something
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        dismissDialog(DIALOG_PROGRESS);
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
                        AVService.getInstance(ReportDetailsActivity.this).cancelTask();
                        finish();
                    }
                });
                pd.setMessage(getString(R.string.ui_message_loading));
                return pd;

            default:
                return super.onCreateDialog(id);
        }
    }
}
