package com.fabernovel.alertevoirie.webservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.fabernovel.alertevoirie.R;
import com.fabernovel.alertevoirie.entities.Constants;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

@SuppressWarnings("unused")
public class AVService {
    public static final int     REQUEST_ERROR  = 0;
    public static final int     REQUEST_JSON   = 1;

    private static final String AV_URL_PREPROD = "http://alerte-voirie.ppd.c4mprod.com/api/";
    private static final String AV_URL_PROD    = "http://www.alertevoirie.com/api/";
    private static final String URL_TEST       = "http://test.dev.playtomo.com/tools/testpost.php";

    private static final String AV_URL         = AV_URL_PREPROD;

    private static final String AV_IMG_FAR     = "far";
    private static final String AV_IMG_CLOSE   = "close";

    private Context             context;
    private static AVService    instance       = null;
    private RequestListener     listener       = null;
    private AsyncTask<?, ?, ?>  currentTask;

    public static AVService getInstance(Context c) {
        if (instance == null) {
            instance = new AVService();
        }
        instance.context = c;
        return instance;
    }

    public void postJSON(JSONArray json, RequestListener listener) {
        this.listener = listener;
        cancelTask();
        currentTask = new QueryTask().execute(json.toString(), AV_URL);
    }

    public void cancelTask() {
        if (currentTask != null && currentTask.getStatus() == AsyncTask.Status.RUNNING) {
            currentTask.cancel(true);
        }
    }

    private class QueryTask extends AsyncTask<String, Void, String> {
        private static final int        PARAM_JSON = 0;
        private static final int        PARAM_URL  = 1;
        private AVServiceErrorException exception;

        @Override
        protected String doInBackground(String... params) {
            HttpPostRequest req = new HttpPostRequest(params[PARAM_URL]);
            req.addParam(HttpPostRequest.PARAM_JSON, params[PARAM_JSON]);
            try {
                return req.sendRequest();
            } catch (AVServiceErrorException e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (exception == null) {
                listener.onRequestcompleted(REQUEST_JSON, result);
            } else {
                toastServerError();

                listener.onRequestcompleted(REQUEST_ERROR, exception);

            }
        }

    }

    /**
     * Post the image related to the incident, with httpPost method. The comment has to be encoded in base64
     * 
     * @param udid
     *            The device id
     * @param img_comment
     *            The far image comment
     * @param incident_id
     *            The id of the related incident
     * @param image_far
     *            The file containing far image
     * @param image_near
     *            The file containing close image
     */
    @SuppressWarnings("unchecked")
    public void postImage(String udid, String img_comment, String incident_id, File image_far, File image_near) {

        ArrayList<Object> image_1 = new ArrayList<Object>();
        ArrayList<Object> image_2 = new ArrayList<Object>();

        image_1.add(AV_URL + "photo/");
        image_1.add(udid);
        try {
            image_1.add(Base64.encodeToString(img_comment.getBytes("UTF-8"), Base64.NO_WRAP));// .replace("=", "%3D"));
        } catch (UnsupportedEncodingException e) {
            Log.e(Constants.PROJECT_TAG, "UTF-8 not supported", e);
            image_1.add(Base64.encodeToString(img_comment.getBytes(), Base64.NO_WRAP));// .replace("=", "%3D"));
        }
        image_1.add(incident_id);
        image_1.add(AV_IMG_FAR);
        image_1.add(image_far);

        image_2.add(AV_URL + "photo/");
        image_2.add(udid);
        image_2.add("");
        image_2.add(incident_id);
        image_2.add(AV_IMG_CLOSE);
        image_2.add(image_near);

        cancelTask();
        currentTask = new postImage().execute(image_1, image_2);

    }

    public void toastServerError() {
        try {
            toastServerError(((Activity) context).getString(R.string.server_error));
        } catch (Exception e) {
            Log.e(Constants.PROJECT_TAG, "Exception", e);
        }

    }

    public void toastServerError(String message) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(Constants.PROJECT_TAG, "Exception", e);
        }

    }

    private class postImage extends AsyncTask<ArrayList<Object>, Integer, HttpResponse[]> {

        @Override
        protected HttpResponse[] doInBackground(ArrayList<Object>... params) {

            HttpResponse[] response = new HttpResponse[2];
            int i = 0;

            for (ArrayList<Object> PicArray : params) {

                HttpClient httpClient = new DefaultHttpClient();
                HttpContext localContext = new BasicHttpContext();
                HttpPost httpPost = new HttpPost((String) PicArray.get(0));

                httpPost.addHeader("udid", (String) PicArray.get(1));
                Log.d(Constants.PROJECT_TAG, "length : " + ((String) PicArray.get(2)).length());
                httpPost.addHeader("img_comment", (String) PicArray.get(2));
                httpPost.addHeader("incident_id", (String) PicArray.get(3));
                httpPost.addHeader("type", (String) PicArray.get(4));

                try {
                    /*
                     * MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                     * for (int index = 0; index < nameValuePairs.size(); index++) {
                     * if (nameValuePairs.get(index).getName().equalsIgnoreCase("image")) {
                     * // If the key equals to "image", we use FileBody to transfer the data
                     * entity.addPart(nameValuePairs.get(index).getName(), new FileBody(new File(nameValuePairs.get(index).getValue())));
                     * } else {
                     * // Normal string data
                     * entity.addPart(nameValuePairs.get(index).getName(), new StringBody(nameValuePairs.get(index).getValue()));
                     * }
                     * }
                     */

                    /*
                     * MultipartEntity file = new MultipartEntity();
                     * ContentBody cbFile = new FileBody((File) PicArray.get(5), "image/jpeg");
                     * file.addPart("userfile", cbFile);
                     */

                    FileEntity file = new FileEntity((File) PicArray.get(5), "image/jpeg");
                    file.setContentType("image/jpeg");

                    httpPost.setEntity(file);

                    // Log.d(Constants.PROJECT_TAG,convertStreamToString(httpPost.getEntity().getContent()));

                    response[i++] = httpClient.execute(httpPost, localContext);

                } catch (IOException e) {
                    Log.e(Constants.PROJECT_TAG, "IOException postImage", e);
                } catch (IllegalStateException e) {
                    Log.e(Constants.PROJECT_TAG, "IllegalStateException postImage", e);
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(HttpResponse[] result) {

            try {
                Log.d(Constants.PROJECT_TAG, "Result post image 1: " + convertStreamToString(result[0].getEntity().getContent()));
                Log.d(Constants.PROJECT_TAG, "Result post image 2: " + convertStreamToString(result[1].getEntity().getContent()));

            } catch (IllegalStateException e) {
                Log.e(Constants.PROJECT_TAG, "IllegalStateException", e);
            } catch (AVServiceErrorException e) {
                Log.e(Constants.PROJECT_TAG, "AVServiceErrorException", e);
            } catch (IOException e) {
                Log.e(Constants.PROJECT_TAG, "IOException", e);
            }
            super.onPostExecute(result);
        }
    }

    private String convertStreamToString(final InputStream is) throws AVServiceErrorException {
        final StringBuilder sb = new StringBuilder();
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + '\n');
            }
        } catch (final IOException e) {
            throw new AVServiceErrorException(999);
        } finally {
            try {
                is.close();
            } catch (final IOException e) {
                throw new AVServiceErrorException(999);
            }
        }
        return sb.toString();
    }
}
