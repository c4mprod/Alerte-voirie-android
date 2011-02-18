package com.fabernovel.alertevoirie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Button;
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

    private static final int    ACTION_SOLVE_INCIDENT       = 1;
    private static final int    ACTION_CONFIRM_INCIDENT     = 2;

    private static final int    ACTION_INVALID_INCIDENT     = 3;

    private boolean             hasPic;

    private Uri                 uriOfPicFromCamera;

    private Incident            currentIncident             = new Incident();

    private String              img_comment                 = "";

    private boolean             canvalidate                 = false;

    private ProgressDialog      mPd;

    private int                 mCurrentAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_report_details);

        // init title
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_nouveau_rapport);
        getWindow().setTitle(getString(R.string.report_detail_new_report_title));

        findViewById(R.id.LinearLayout_comment).setVisibility(View.GONE);
        ((Button) findViewById(R.id.Button_validate)).setEnabled(false);

        File img_close = new File(getFilesDir() + "/" + CAPTURE_CLOSE);
        File img_far = new File(getFilesDir() + "/" + CAPTURE_ARROW);
        File img_far2 = new File(getFilesDir() + "/" + CAPTURE_FAR);

        img_close.delete();
        img_far.delete();
        img_far2.delete();

        if (getIntent().getBooleanExtra("existing", false)) {
            findViewById(R.id.Button_validate).setVisibility(View.GONE);
            findViewById(R.id.existing_incidents_layout).setVisibility(View.VISIBLE);
            try {
                ImageDownloader imgd = new ImageDownloader(this);
                currentIncident = Incident.fromJSONObject(this, new JSONObject(getIntent().getStringExtra("event")));
                setCategory(currentIncident.categoryId);
                ((TextView) findViewById(R.id.TextView_comment)).setText(currentIncident.description);
                ((TextView) findViewById(R.id.TextView_address)).setText(currentIncident.address);
                // imgd.setDefault_img(((ImageView) findViewById(R.id.ImageView_far)).getBackground());

                findViewById(R.id.existing_incident_solved).setOnClickListener(this);
                findViewById(R.id.existing_incidents_confirmed).setOnClickListener(this);
                findViewById(R.id.existing_incidents_add_picture).setOnClickListener(this);
                findViewById(R.id.existing_incidents_invalid).setOnClickListener(this);

                if (currentIncident.description != null && currentIncident.description.length() > 0) {
                    findViewById(R.id.TextView_nocomment).setVisibility(View.GONE);
                    findViewById(R.id.LinearLayout_comment).setVisibility(View.VISIBLE);
                }

                if (currentIncident.confirms > 1) {
                    ((TextView) findViewById(R.id.existing_incident_status)).setText(currentIncident.confirms + " personnes confirment cet incident");
                } else if (currentIncident.confirms == 1) {
                    ((TextView) findViewById(R.id.existing_incident_status)).setText(currentIncident.confirms + " personne confirme cet incident");
                }

                imgd.download((String) currentIncident.pictures_far.get(0), ((ImageView) findViewById(R.id.ImageView_far)));
                if (currentIncident.pictures_close.length() > 0) {
                    imgd.download((String) currentIncident.pictures_close.get(0), ((ImageView) findViewById(R.id.ImageView_close)));
                }

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
        // AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // AlertDialog alert;
        Log.d(Constants.PROJECT_TAG, "onClick : " + v.getId());
        mCurrentAction = -1;
        switch (v.getId()) {
            case R.id.ImageView_close:
                final ActionItem actionPhoto = new ActionItem();
                final ActionItem actionGallery = new ActionItem();
                final QuickAction qax = new QuickAction(v);

                actionPhoto.setTitle("Prendre une photo");
                // chart.setIcon(getResources().getDrawable(R.drawable.chart));
                actionPhoto.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takePicture(0, R.id.ImageView_close);
                        qax.dismiss();
                    }
                });

                actionGallery.setTitle("Choisir un fichier");
                // production.setIcon(getResources().getDrawable(R.drawable.production));
                actionGallery.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takePicture(1, R.id.ImageView_close);
                        qax.dismiss();

                    }
                });

                qax.addActionItem(actionPhoto);
                qax.addActionItem(actionGallery);
                qax.setAnimStyle(QuickAction.ANIM_AUTO);

                qax.show();
                break;
            case R.id.ImageView_far:

                if (hasPic) {
                    final QuickAction qa = new QuickAction(v);
                    final ActionItem actionNew = new ActionItem();
                    final ActionItem actionNew2 = new ActionItem();
                    final ActionItem actionModif = new ActionItem();

                    actionNew.setTitle("Nouvelle image\n(APN)");
                    actionNew.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            takePicture(0, R.id.ImageView_far);
                            qa.dismiss();

                        }
                    });

                    actionNew2.setTitle("Nouvelle image\n(Galerie)");
                    actionNew2.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            takePicture(1, R.id.ImageView_far);
                            qa.dismiss();
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

                    qa.addActionItem(actionNew);
                    qa.addActionItem(actionNew2);
                    qa.addActionItem(actionModif);
                    qa.setAnimStyle(QuickAction.ANIM_AUTO);

                    qa.show();
                } else {
                    final ActionItem actionNew = new ActionItem();
                    final ActionItem actionModif = new ActionItem();
                    final QuickAction qa = new QuickAction(v);

                    actionNew.setTitle("Prendre une photo");
                    // chart.setIcon(getResources().getDrawable(R.drawable.chart));
                    actionNew.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            takePicture(0, R.id.ImageView_far);
                            qa.dismiss();
                        }
                    });

                    actionModif.setTitle("Choisir un fichier");
                    // production.setIcon(getResources().getDrawable(R.drawable.production));
                    actionModif.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            takePicture(1, R.id.ImageView_far);
                            qa.dismiss();

                        }
                    });

                    qa.addActionItem(actionNew);
                    qa.addActionItem(actionModif);
                    qa.setAnimStyle(QuickAction.ANIM_AUTO);

                    qa.show();
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
                mCurrentAction = ACTION_SOLVE_INCIDENT;
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == Dialog.BUTTON_POSITIVE) {
                            UpdateIncident(JsonData.PARAM_UPDATE_INCIDENT_RESOLVED);
                        }
                        dialog.dismiss();
                    }
                };
                new AlertDialog.Builder(this).setMessage(R.string.report_detail_confirm_question)
                                             .setCancelable(false)
                                             .setPositiveButton(R.string.oui, listener)
                                             .setNegativeButton(R.string.non, listener).show();
                break;
            case R.id.existing_incidents_confirmed:
                mCurrentAction = ACTION_CONFIRM_INCIDENT;
                UpdateIncident(JsonData.PARAM_UPDATE_INCIDENT_CONFIRMED);
                // Nico : show this after request is complete !
                // builder.setMessage(R.string.report_detail_new_report_ok).setCancelable(false).setPositiveButton("Ok", null);
                // alert = builder.create();
                // alert.show();
                break;
            case R.id.existing_incidents_add_picture:
                final ActionItem actionNew = new ActionItem();
                final ActionItem actionModif = new ActionItem();
                final QuickAction qa = new QuickAction(v);

                actionNew.setTitle("Prendre une photo");
                // chart.setIcon(getResources().getDrawable(R.drawable.chart));
                actionNew.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takePicture(0, R.id.existing_incidents_add_picture);
                        finish();
                    }
                });

                actionModif.setTitle("Choisir un fichier");
                // production.setIcon(getResources().getDrawable(R.drawable.production));
                actionModif.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takePicture(1, R.id.existing_incidents_add_picture);
                        finish();

                    }
                });

                qa.addActionItem(actionNew);
                qa.addActionItem(actionModif);
                qa.setAnimStyle(QuickAction.ANIM_AUTO);

                qa.show();

                break;
            case R.id.existing_incidents_invalid:
                UpdateIncident(JsonData.PARAM_UPDATE_INCIDENT_INVALID);
                mCurrentAction = ACTION_INVALID_INCIDENT;
                // builder.setMessage(R.string.report_detail_new_report_ok).setCancelable(false).setPositiveButton("Ok", null);
                // alert = builder.create();
                // alert.show();
                break;
            case R.id.Button_validate:

                loadComment(REQUEST_COMMENT_BEFORE_EXIT);
                //$FALL-THROUGH$
            default:
                break;

        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
        canvalidate = true;
        findViewById(R.id.LinearLayout_comment).setVisibility(View.VISIBLE);
        Log.d("AlerteVoirie_PM", "launch the damn thing !");
        Intent i = new Intent(ReportDetailsActivity.this, SelectZoomDetail.class);
        i.putExtra("comment", img_comment);
        startActivityForResult(i, REQUEST_DETAILS);

    }

    private void takePicture(int type, int RequestCode) {
        // Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("capture", ".tmp");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        uriOfPicFromCamera = Uri.fromFile(tmpFile);
        camIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriOfPicFromCamera);

        Intent gallIntent = new Intent();
        gallIntent.setType("image/*");
        gallIntent.setAction(Intent.ACTION_GET_CONTENT);
        // gallIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriOfPicFromCamera);

        // List<Intent> yourIntentsList = new ArrayList<Intent>();
        //
        // List<ResolveInfo> listCam = getPackageManager().queryIntentActivities(camIntent, 0);
        // for (ResolveInfo res : listCam) {
        // final Intent finalIntent = new Intent(camIntent);
        // finalIntent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
        // yourIntentsList.add(finalIntent);
        // }
        //
        // List<ResolveInfo> listGall = getPackageManager().queryIntentActivities(gallIntent, 0);
        // for (ResolveInfo res : listGall) {
        // final Intent finalIntent = new Intent(gallIntent);
        // finalIntent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
        // yourIntentsList.add(finalIntent);
        // }

        if (type == 0) {

            ReportDetailsActivity.this.startActivityForResult(camIntent, RequestCode);
        } else if (type == 1) {

            ReportDetailsActivity.this.startActivityForResult(Intent.createChooser(gallIntent, "Galerie photo"), RequestCode);
        }
        // startActivityForResult(intent, v.getId());

    }

    // @Override
    // public boolean onCreateOptionsMenu(Menu menu) {
    //
    // File tmpFile = null;
    // try {
    // tmpFile = File.createTempFile("capture", "tmp");
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    //
    // Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // uriOfPicFromCamera = Uri.fromFile(tmpFile);
    // camIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriOfPicFromCamera);
    // camIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
    //
    // Intent gallIntent = new Intent();
    // gallIntent.setType("image/*");
    // gallIntent.setAction(Intent.ACTION_GET_CONTENT);
    // gallIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
    //
    // // Create an Intent that describes the requirements to fulfill, to be included
    // // in our menu. The offering app must include a category value of Intent.CATEGORY_ALTERNATIVE.
    //
    // // Search and populate the menu with acceptable offering applications.
    // menu.addIntentOptions(0, // Menu group to which new items will be added
    // 0, // Unique item ID (none)
    // 0, // Order for the items (none)
    // this.getComponentName(), // The current Activity name
    // null, // Specific items to place first (none)
    // camIntent, // Intent created above that describes our requirements
    // 0, // Additional flags to control items (none)
    // null); // Array of MenuItems that correlate to specific items (none)
    //
    // // Search and populate the menu with acceptable offering applications.
    // menu.addIntentOptions(0, // Menu group to which new items will be added
    // 1, // Unique item ID (none)
    // 1, // Order for the items (none)
    // this.getComponentName(), // The current Activity name
    // null, // Specific items to place first (none)
    // gallIntent, // Intent created above that describes our requirements
    // 0, // Additional flags to control items (none)
    // null); // Array of MenuItems that correlate to specific items (none)
    //
    // return super.onCreateOptionsMenu(menu);
    // }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(Constants.PROJECT_TAG, "Result : " + requestCode);
        switch (requestCode) {
            case R.id.existing_incidents_add_picture:
            case R.id.ImageView_far:
            case R.id.ImageView_close:
                if (resultCode == RESULT_OK) {
                    try {

                        String finalPath;

                        if (data != null) {
                            Uri path = data.getData();
                            // OI FILE Manager
                            String filemanagerString = path.getPath();
                            // MEDIA GALLERY
                            String selectedImagePath = getPath(path);

                            if (selectedImagePath != null) {
                                finalPath = selectedImagePath;
                                System.out.println("selectedImagePath is the right one for you! " + finalPath);
                            } else {
                                finalPath = filemanagerString;
                                System.out.println("filemanagerstring is the right one for you!" + finalPath);
                            }
                            // boolean isImage = true;
                        } else {
                            finalPath = uriOfPicFromCamera.getPath();
                        }

                        // if (data == null || getMimeType(finalPath).startsWith("image")) {
                        InputStream in;
                        BitmapFactory.Options opt = new BitmapFactory.Options();

                        // get the sample size to have a smaller image
                        in = getContentResolver().openInputStream(Uri.fromFile(new File(finalPath)));
                        opt.inSampleSize = getSampleSize(getContentResolver().openInputStream(Uri.fromFile(new File(finalPath))));
                        in.close();

                        // decode a sampled version of the picture
                        in = getContentResolver().openInputStream(Uri.fromFile(new File(finalPath)));
                        Bitmap picture = BitmapFactory.decodeStream(in, null, opt);

                        // Bitmap picture = BitmapFactory.decodeFile(finalPath);
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
                            if (requestCode == R.id.ImageView_far) {
                                loadZoom();
                            }
                            if (requestCode == R.id.ImageView_far && ((TextView) findViewById(R.id.TextView_address)).getText().length() > 0) {
                                ((Button) findViewById(R.id.Button_validate)).setEnabled(true);
                            }
                        } else {

                            showDialog(DIALOG_PROGRESS);
                            File img_close = new File(getFilesDir() + "/" + CAPTURE_CLOSE);

                            AVService.getInstance(this).postImage(Utils.getUdid(this), null, Long.toString(currentIncident.id), null, img_close);
                        }
                        // }

                        // FileOutputStream fos = openFileOutput("capture", MODE_WORLD_READABLE);
                        // InputStream in = getContentResolver().openInputStream(uriOfPicFromCamera);
                        // Utils.fromInputToOutput(in, fos);
                        // fos.close();
                        // in.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        AlertDialog alert;
                        builder.setMessage("Image invalide").setCancelable(false).setPositiveButton("Ok", null);
                        alert = builder.create();
                        alert.show();
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
                    if (currentIncident.address != null && currentIncident.address.length() > 0 && canvalidate) {
                        ((Button) findViewById(R.id.Button_validate)).setEnabled(true);
                    }
                }
                break;
            case REQUEST_COMMENT:
                if (resultCode == RESULT_OK) {
                    currentIncident.description = data.getStringExtra(IntentData.EXTRA_COMMENT);
                    ((TextView) findViewById(R.id.TextView_comment)).setText(currentIncident.description);
                    if (currentIncident.description != null) findViewById(R.id.TextView_nocomment).setVisibility(View.GONE);
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
                    // startActivityForResult(data, requestCode)
                    
                    //set new img
                    setPictureToImageView("arrowed.jpg", (ImageView) findViewById(R.id.ImageView_far));
                    
                    loadComment(REQUEST_COMMENT);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public String getMimeType(String fileUrl) throws java.io.IOException {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String type = fileNameMap.getContentTypeFor(fileUrl);

        if (type == null) return "unknown";

        return type;
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
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

            //WTF ?
//            if (hasPic && (imageView.getId() == R.id.ImageView_far)) {
//                loadZoom();
//            }

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
        if (mPd != null && mPd.isShowing()) {
            // clear pd from memory to avoid progress bar freeze when showed again
            removeDialog(DIALOG_PROGRESS);
        }

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

                    if (!img_far.exists()) {
                        img_far = new File(getFilesDir() + "/" + CAPTURE_FAR);
                    }

                    AVService.getInstance(this).postImage(Utils.getUdid(this), img_comment,
                                                          answer.getJSONObject(JsonData.PARAM_ANSWER).getString(JsonData.ANSWER_INCIDENT_ID), img_far,
                                                          img_close);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    AlertDialog alert;
                    builder.setMessage(R.string.report_detail_new_report_ok).setCancelable(false).setPositiveButton("Ok", null);
                    alert = builder.create();
                    alert.show();
                } else {
                    Log.d("AlerteVoirie_PM", "answer : " + answer);

                    // hotfix nico : here we can have valid answer for incident updates !!!
                    // handle answer and display popup here instead of when we click on buttons
                    int statuscode = answer.getJSONObject(JsonData.PARAM_ANSWER).getInt(JsonData.PARAM_STATUS);

                    if (statuscode == 0) {
                        // FIXME end activity when resolve incident ??
                        switch (mCurrentAction) {
                            case ACTION_CONFIRM_INCIDENT:
                                new AlertDialog.Builder(this).setMessage(R.string.news_incidents_confirmed).setPositiveButton(android.R.string.ok, null).show();
                                break;
                            case ACTION_INVALID_INCIDENT: {
                                AlertDialog dialog = new AlertDialog.Builder(this).setMessage(R.string.news_incidents_invalidated)
                                                                                  .setPositiveButton(android.R.string.ok, null)
                                                                                  .create();
                                dialog.setOnDismissListener(new OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        finish();
                                    }
                                });
                                dialog.show();
                                break;
                            }
                            case ACTION_SOLVE_INCIDENT: {
                                AlertDialog dialog = new AlertDialog.Builder(this).setMessage(R.string.news_incidents_resolved)
                                                                                  .setPositiveButton(android.R.string.ok, null)
                                                                                  .create();
                                dialog.setOnDismissListener(new OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        finish();
                                    }
                                });
                                dialog.show();
                                break;
                            }
                            default:
                                // assume it's a generic update request in other cases ...
                                new AlertDialog.Builder(this).setMessage(R.string.report_detail_update_ok).setPositiveButton(android.R.string.ok, null).show();
                                break;
                        }
                    } else {
                        Log.d("AlerteVoirie_PM", "json de ouf : "+result);
                        // other things
                        // FIXME show popups instead of toasts !
                        if ((answer.getJSONObject(JsonData.PARAM_ANSWER).getInt(JsonData.PARAM_STATUS)) == 18) {
                            Toast.makeText(this, getString(R.string.incident_already_confirmed), Toast.LENGTH_LONG).show();
                        } else {
                            Log.d("AlerteVoirie_PM", "erreur ?");
                            Toast.makeText(this, getString(R.string.server_error), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(Constants.PROJECT_TAG, "Erreur d'envoi d'image", e);
            }/*
              * catch (FileNotFoundException e) {
              * // TODO Auto-generated catch block
              * Log.e(Constants.PROJECT_TAG,"File not found",e);
              * }
              */

        }

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PROGRESS:
                mPd = new ProgressDialog(this);
                mPd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mPd.setIndeterminate(true);
                mPd.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeDialog(DIALOG_PROGRESS);
                    }
                });
                mPd.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        AVService.getInstance(ReportDetailsActivity.this).cancelTask();
                        finish();
                    }
                });
                mPd.setMessage(getString(R.string.ui_message_loading));
                return mPd;

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
