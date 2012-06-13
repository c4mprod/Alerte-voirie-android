/**
 * This file is part of the Alerte Voirie project.
 * 
 * Copyright (C) 2010-2011 C4M PROD
 * 
 * Alerte Voirie is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alerte Voirie is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alerte Voirie.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.fabernovel.alertevoirie.webservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.util.Log;

import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.JsonData;

public class HttpPostRequest {
    private static final String   HEADER_APP_REQUEST_SIGNATURE = "x-app-request-signature";
    private static final String   HEADER_APP_DEVICE_MODEL      = "x-app-device-model";
    private static final String   HEADER_APP_PLATFORM          = "x-app-platform";
    private static final String   HEADER_APP_VERSION           = "x-app-version";

    private static final String   MAGIC_KEY                    = "TBD";

    protected static final String PARAM_JSON                   = "jsonStream";

    private String                url;
    private HttpPost              httpPost;
    private InputStream           content;
    private String                contentString;

    private List<NameValuePair>   params                       = new ArrayList<NameValuePair>();

    public HttpPostRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void addParam(String paramName, String paramValue) {
        params.add(new BasicNameValuePair(paramName, paramValue));
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
    
    public String sendRequest() throws AVServiceErrorException {
        try {

            httpPost = new HttpPost(url);

            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            int timeoutConnection = 20000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 5000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            

            DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);

            httpPost.setEntity(new UrlEncodedFormEntity(params,HTTP.UTF_8));

            httpPost.addHeader(HEADER_APP_VERSION, "1.0.0");
            httpPost.addHeader(HEADER_APP_PLATFORM, "android_family");
            httpPost.addHeader(HEADER_APP_DEVICE_MODEL, (Build.MANUFACTURER + " " + Build.DEVICE).trim());
            httpPost.addHeader(HEADER_APP_REQUEST_SIGNATURE, sha1(MAGIC_KEY + params.get(0).getValue()));

            // Log.i(Constants.PROJECT_TAG,MAGIC_KEY + params.get(0).getValue());

            final HttpResponse response = httpClient.execute(httpPost);
            final HttpEntity entity = response.getEntity();
            content = entity.getContent();
            contentString = convertStreamToString(content);
            Log.d(Constants.PROJECT_TAG, "answer = " + contentString);
        } catch (final UnsupportedEncodingException uee) {
            Log.e(Constants.PROJECT_TAG, "UnsupportedEncodingException", uee);
            throw new AVServiceErrorException(999);
        } catch (final IOException ioe) {
            Log.e(Constants.PROJECT_TAG, "IOException", ioe);
            throw new AVServiceErrorException(999);
        } catch (final IllegalStateException ise) {
            Log.e(Constants.PROJECT_TAG, "IllegalStateException", ise);
            throw new AVServiceErrorException(999);
        } catch (NoSuchAlgorithmException e) {
            Log.e(Constants.PROJECT_TAG, "NoSuchAlgorithmException", e);
            throw new AVServiceErrorException(999);
        } catch (Exception e)
        {
            Log.e(Constants.PROJECT_TAG, "error in sendRequest : ", e);
            throw new AVServiceErrorException(999);
        }
        

        try {
            
            Log.d(Constants.PROJECT_TAG,"contenString: "+contentString);
            JSONObject jo = new JSONObject(contentString);
            int resultnum = jo.getJSONObject(JsonData.PARAM_ANSWER).getInt(JsonData.PARAM_STATUS);
            Log.i(Constants.PROJECT_TAG, "AV Status:" + resultnum);
            if (resultnum != 0) throw new AVServiceErrorException(resultnum);
            
            

        } catch (JSONException e) {
            Log.w(Constants.PROJECT_TAG, "JSONException in onPostExecute");
            //throw new AVServiceErrorException(999);
        }

        return contentString;
    }

    private static String sha1(String raw) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytes = raw.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance(MGF1ParameterSpec.SHA1.getDigestAlgorithm());
        md.update(bytes);
        byte[] digest = md.digest();

        return toHex(digest);
    }

    private static String toHex(byte[] buf) {
        if (buf == null) return "";
        StringBuffer result = new StringBuffer(2 * buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(Integer.toHexString((b >> 4) & 0x0f)).append(Integer.toHexString(b & 0x0f));
    }
}
