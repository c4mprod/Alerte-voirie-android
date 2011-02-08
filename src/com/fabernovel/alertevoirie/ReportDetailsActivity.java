package com.fabernovel.alertevoirie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.c4mprod.utils.ImageDownloader;
import com.c4mprod.utils.ImageDownloaderListener;
import com.fabernovel.alertevoirie.entities.Category;
import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.Incident;
import com.fabernovel.alertevoirie.entities.IntentData;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.utils.Utils;
import com.fabernovel.alertevoirie.webservice.AVService;
import com.fabernovel.alertevoirie.webservice.RequestListener;

public class ReportDetailsActivity extends Activity implements OnClickListener, RequestListener, ImageDownloaderListener {
    private static final String EMPTY_STRING                = "";

    private static final int    REQUEST_CATEGORY            = 0;
    private static final int    REQUEST_POSITION            = 1;
    private static final int    REQUEST_COMMENT             = 2;
    private static final int    REQUEST_DETAILS             = 3;

    public static final String  CAPTURE_FAR                 = "capture_far.jpg";
    public static final String  CAPTURE_CLOSE               = "capture_close.jpg";
    public static final String  CAPTURE_ARROW               = "arrowed.jpg";

    private static final int    DIALOG_PROGRESS             = 0;

    private static final int    REQUEST_COMMENT_BEFORE_EXIT = 4;
    private boolean             hasPic;

    private Uri                 uriOfPicFromCamera;

    private Incident            currentIncident             = new Incident();

    private String              img_comment                 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_report_details);

        // init title
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_nouveau_rapport);
        getWindow().setTitle(getString(R.string.report_detail_new_report_title));

        if (getIntent().getBooleanExtra("existing", false)) {
            findViewById(R.id.Button_validate).setVisibility(View.GONE);
            findViewById(R.id.existing_incidents_layout).setVisibility(View.VISIBLE);
            try {
                ImageDownloader imgd = new ImageDownloader(this);
                currentIncident = Incident.fromJSONObject(new JSONObject(getIntent().getStringExtra("event")));
                setCategory(currentIncident.categoryId);
                ((TextView) findViewById(R.id.TextView_comment)).setText(currentIncident.description);
                ((TextView) findViewById(R.id.TextView_address)).setText(currentIncident.address);
                // imgd.setDefault_img(((ImageView) findViewById(R.id.ImageView_far)).getBackground());
                

                findViewById(R.id.existing_incident_solved).setOnClickListener(this);
                findViewById(R.id.existing_incidents_confirmed).setOnClickListener(this);
                findViewById(R.id.existing_incidents_add_picture).setOnClickListener(this);
                findViewById(R.id.existing_incidents_invalid).setOnClickListener(this);
                
                if(currentIncident.confirms>1){
                    ((TextView)findViewById(R.id.existing_incident_status)).setText(currentIncident.confirms+" personnes confirment cet incident");
                }else if(currentIncident.confirms==1){
                    ((TextView)findViewById(R.id.existing_incident_status)).setText(currentIncident.confirms+" personne confirme cet incident");
                }
                
                imgd.download((String) currentIncident.pictures_close.get(0), ((ImageView) findViewById(R.id.ImageView_close)));
                imgd.download((String) currentIncident.pictures_far.get(0), ((ImageView) findViewById(R.id.ImageView_far)));

            } catch (JSONException e) {
                Log.e(Constants.PROJECT_TAG, "JSONException in onCreate", e);
            }

        } else {
            findViewById(R.id.ImageView_far).setOnClickListener(this);
            findViewById(R.id.ImageView_close).setOnClickListener(this);
            findViewById(R.id.LinearLayout_category).setOnClickListener(this);
            findViewById(R.id.LinearLayout_where).setOnClickListener(this);
            findViewById(R.id.LinearLayout_comment).setOnClickListener(this);
            findViewById(R.id.Button_validate).setOnClickListener(this);
        }
        // init buttons

        if (getIntent().getLongExtra(IntentData.EXTRA_CATEGORY_ID, -1) != -1) {
            setCategory(getIntent().getLongExtra(IntentData.EXTRA_CATEGORY_ID, 0));
            startActivityForResult(new Intent(this, SelectPositionActivity.class), REQUEST_POSITION);
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(Constants.PROJECT_TAG, "onClick : "+v.getId());
        switch (v.getId()) {
            case R.id.ImageView_close:
                takePicture(v);
                break;
            case R.id.ImageView_far:

                if (hasPic) {
                    final ActionItem actionNew = new ActionItem();
                    final ActionItem actionModif = new ActionItem();
                    final View button = v;

                    actionNew.setTitle("Nouvelle image");
                    // chart.setIcon(getResources().getDrawable(R.drawable.chart));
                    actionNew.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            takePicture(button);
                        }
                    });

                    actionModif.setTitle("Préciser l'anomalie");
                    // production.setIcon(getResources().getDrawable(R.drawable.production));
                    actionModif.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            loadZoom();

                        }
                    });

                    QuickAction qa = new QuickAction(findViewById(R.id.AnchorZoom));

                    qa.addActionItem(actionNew);
                    qa.addActionItem(actionModif);
                    qa.setAnimStyle(QuickAction.ANIM_AUTO);

                    qa.show();
                } else {
                    takePicture(v);
                }

                break;

            case R.id.LinearLayout_category:
                startActivityForResult(new Intent(this, SelectCategoryActivity.class), REQUEST_CATEGORY);
                break;
            case R.id.LinearLayout_where:
                startActivityForResult(new Intent(this, SelectPositionActivity.class), REQUEST_POSITION);
                break;
            case R.id.LinearLayout_comment:
                loadComment(REQUEST_COMMENT);
                break;

            case R.id.existing_incident_solved:
                UpdateIncident(JsonData.PARAM_UPDATE_INCIDENT_RESOLVED);
                Toast.makeText(this, getString(R.string.report_detail_new_report_ok), Toast.LENGTH_LONG).show();
                finish();
                break;
            case R.id.existing_incidents_confirmed:
                UpdateIncident(JsonData.PARAM_UPDATE_INCIDENT_CONFIRMED);
                Toast.makeText(this, getString(R.string.report_detail_new_report_ok), Toast.LENGTH_LONG).show();
                finish();
                break;
            case R.id.existing_incidents_add_picture:
                takePicture(v);
                finish();
                break;
            case R.id.existing_incidents_invalid:
                UpdateIncident(JsonData.PARAM_UPDATE_INCIDENT_INVALID);
                Toast.makeText(this, getString(R.string.report_detail_new_report_ok), Toast.LENGTH_LONG).show();
                finish();
                break;
            case R.id.Button_validate:

                loadComment(REQUEST_COMMENT_BEFORE_EXIT);
                //$FALL-THROUGH$
            default:
                break;
        }
    }

    private void loadComment(int what) {

        if (what == REQUEST_COMMENT_BEFORE_EXIT && ((TextView) findViewById(R.id.TextView_comment)).getText().toString().length() > 0) {
            postNewIncident();
        } else {
            Intent intent2 = new Intent(this, AddCommentActivity.class);
            intent2.putExtra(IntentData.EXTRA_COMMENT, ((TextView) findViewById(R.id.TextView_comment)).getText().toString());
            startActivityForResult(intent2, what);
        }

    }

    protected void loadZoom() {
        Intent i = new Intent(ReportDetailsActivity.this, SelectZoomDetail.class);
        i.putExtra("comment", img_comment);
        startActivityForResult(i, REQUEST_DETAILS);

    }

    private void takePicture(View v) {
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

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(Constants.PROJECT_TAG, "Result : " + requestCode);
        switch (requestCode) {
            case R.id.existing_incidents_add_picture:
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

                        File f = new File(uriOfPicFromCamera.getPath());
                        f.delete();

                        // save the new image
                        String pictureName = requestCode == R.id.ImageView_far ? CAPTURE_FAR : CAPTURE_CLOSE;
                        FileOutputStream fos = openFileOutput(pictureName, MODE_PRIVATE);
                        picture.compress(CompressFormat.JPEG, 80, fos);
                        fos.close();

                        if (requestCode != R.id.existing_incidents_add_picture) {
                            setPictureToImageView(pictureName, (ImageView) findViewById(requestCode));
                        } else {
                            showDialog(DIALOG_PROGRESS);
                            File img_close = new File(getFilesDir() + "/" + CAPTURE_CLOSE);

                            AVService.getInstance(this).postImage(Utils.getUdid(this), img_comment, "" + currentIncident.id, null, img_close);
                        }
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
                    setCategory(data.getLongExtra(IntentData.EXTRA_CATEGORY_ID, -1));

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
                break;
            case REQUEST_COMMENT_BEFORE_EXIT:
                if (resultCode == RESULT_OK) {
                    currentIncident.description = data.getStringExtra(IntentData.EXTRA_COMMENT);
                    ((TextView) findViewById(R.id.TextView_comment)).setText(currentIncident.description);
                    postNewIncident();
                }
                break;
            case REQUEST_DETAILS:
                if (resultCode == RESULT_OK) {
                    img_comment = data.getStringExtra("comment");
                }
                break;
            default:
                break;
        }
    }

    private void setCategory(long l) {
        String subCategories = EMPTY_STRING, category = EMPTY_STRING;
        long catId = l;
        Log.d(Constants.PROJECT_TAG, "Cat id = " + catId);
        currentIncident.categoryId = catId;

        do {
            Cursor c = getContentResolver().query(ContentUris.withAppendedId(Category.CONTENT_URI, catId), new String[] { Category.PARENT, Category.NAME },
                                                  null, null, null);

            if (c.moveToFirst()) {
                catId = c.getInt(c.getColumnIndex(Category.PARENT));
                if (catId != 0) {
                    subCategories = c.getString(c.getColumnIndex(Category.NAME)) + (subCategories.length() > 0 ? ", " + subCategories : EMPTY_STRING);
                } else {
                    category = c.getString(c.getColumnIndex(Category.NAME));
                }
            }
            c.close();
        } while (catId > 0);

        ((TextView) findViewById(R.id.TextView_sub_categories)).setText(subCategories);
        ((TextView) findViewById(R.id.TextView_main_category)).setText(category);

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
                m.postRotate(-90);
                picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), m, true);
            }
            picture = Bitmap.createScaledBitmap(picture, d.getIntrinsicWidth(), d.getIntrinsicHeight(), true);

            d.setDrawableByLayerId(R.id.picture, new BitmapDrawable(picture));
            imageView.setImageDrawable(d);

            if (!hasPic) hasPic = (imageView.getId() == R.id.ImageView_far);

            if (hasPic && (imageView.getId() == R.id.ImageView_far)) {
                loadZoom();
            }
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

    private void UpdateIncident(String Status) {
        JSONObject newIncidentRequest = currentIncident.updateIncidentRequest(this, Status);

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
                Log.d(Constants.PROJECT_TAG, "Request result" + (String) result);
                JSONObject answer = new JSONArray((String) result).getJSONObject(0);
                boolean isIncident = JsonData.VALUE_REQUEST_NEW_INCIDENT.equals(answer.getString(JsonData.PARAM_REQUEST));
                boolean isOk = (JsonData.VALUE_INCIDENT_SAVED == (answer.getJSONObject(JsonData.PARAM_ANSWER).getInt(JsonData.PARAM_STATUS)));

                if (isIncident && isOk) {

                    /*
                     * FileInputStream fis_close = openFileInput(CAPTURE_CLOSE);
                     * FileInputStream fis_far = openFileInput(CAPTURE_FAR);
                     */

                    File img_close = new File(getFilesDir() + "/" + CAPTURE_CLOSE);
                    File img_far = new File(getFilesDir() + "/" + CAPTURE_ARROW);

                    AVService.getInstance(this).postImage(Utils.getUdid(this), img_comment,
                                                          answer.getJSONObject(JsonData.PARAM_ANSWER).getString(JsonData.ANSWER_INCIDENT_ID), img_far,
                                                          img_close);
                    Toast.makeText(this, getString(R.string.report_detail_new_report_ok), Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, getString(R.string.server_error), Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                Log.e(Constants.PROJECT_TAG, "Error uploading image", e);
            }/*
              * catch (FileNotFoundException e) {
              * // TODO Auto-generated catch block
              * Log.e(Constants.PROJECT_TAG,"File not found",e);
              * }
              */
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

    @Override
    public void onImageDownloaded(ImageView imgv, String distant_uri, String local_uri, long width, long height) {
        try {

            if (width / height < 0) {
                RotatePicture(imgv);
            }
        } catch (ArithmeticException e) {
            Log.e(Constants.PROJECT_TAG, "ArithmeticException", e);
        }

    }

    private void RotatePicture(ImageView imageView) {
        Bitmap picture = null;

        picture = ((BitmapDrawable) (imageView.getDrawable())).getBitmap();

        Matrix m = new Matrix();
        m.postRotate(90);
        picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), m, true);

        imageView.setImageBitmap(picture);

    }
}
