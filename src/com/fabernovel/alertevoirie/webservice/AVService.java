package com.fabernovel.alertevoirie.webservice;

import org.json.JSONArray;

import android.content.Context;
import android.os.AsyncTask;

public class AVService {
    public static final int REQUEST_ERROR = 0;
    public static final int REQUEST_JSON = 1;
    
    private static final String AV_URL_PREPROD = "http://alerte-voirie.ppd.c4mprod.com/api/";
    private static final String AV_URL_PROD = "http://alerte-voirie.c4mprod.com/api/";
    private static final String URL_TEST = "http://test.dev.playtomo.com/tools/testpost.php";
    
    private Context context;
    private static AVService instance = null;
    private RequestListener listener = null;
    private AsyncTask<?, ?, ?> currentTask;

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
        currentTask = new QueryTask().execute(json.toString(),AV_URL_PREPROD);
    }
    
    public void cancelTask() {
        if(currentTask != null && currentTask.getStatus() == AsyncTask.Status.RUNNING) {
            currentTask.cancel(true);
        }
    }
    
    private class QueryTask extends AsyncTask<String, Void, String> {
        private static final int PARAM_JSON = 0;
        private static final int PARAM_URL = 1;
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
                listener.onRequestcompleted(REQUEST_ERROR, exception);
            }
        }
        
    }
}
