package com.fabernovel.alertevoirie.data;

import java.io.DataInputStream;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.fabernovel.alertevoirie.R;
import com.fabernovel.alertevoirie.entities.JsonData;
import com.fabernovel.alertevoirie.utils.JSONCursor;

public class CategoryProvider extends ContentProvider {

    private static final int CATEGORIES = 1;
    private static final int CATEGORIES_ID = 2;
    private static final int CATEGORY_ID = 3;

    private static UriMatcher uriMatcher = null;

    public static final String AUTHORITY = "com.fabernovel.alertevoirie.dataprovider.advice";

    public static JSONObject categories;

    @Override
    public boolean onCreate() {
        // load json data
        DataInputStream in = new DataInputStream(getContext().getResources().openRawResource(R.raw.categories));
        try {
            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            categories = (JSONObject) new JSONTokener(new String(buffer)).nextValue();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e("Alerte Voirie", "JSON error", e);
        }
        return true;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read only provider");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read only provider");
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        JSONArray array = new JSONArray();
        try {
            String categoryId = JsonData.VALUE_NULL;
            switch (uriMatcher.match(uri)) {
                case CATEGORIES_ID:
                    categoryId = uri.getLastPathSegment();
                case CATEGORIES:
                    JSONObject parent = categories.getJSONObject(categoryId);
                    if (parent.has(JsonData.PARAM_CATEGORY_CHILDREN)) {
                        JSONArray subset = parent.getJSONArray(JsonData.PARAM_CATEGORY_CHILDREN);
                        for (int i = 0; i < subset.length(); i++) {
                            JSONObject obj = categories.getJSONObject(subset.getString(i));
                            obj.put(BaseColumns._ID, (long)Integer.valueOf(subset.getString(i)));
                            array.put(obj);
                        }
                    }
                    break;
                    
                case CATEGORY_ID:
                    categoryId = uri.getLastPathSegment();
                    array.put(categories.getJSONObject(categoryId));
                    Log.d("Alerte Voirie", "category returned = "+categories.getJSONObject(categoryId));
                    break;

                default:
                    return null;
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Cursor c = new JSONCursor(array, projection);
        if (c != null) c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read only provider");
    }

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "categories", CATEGORIES);
        uriMatcher.addURI(AUTHORITY, "categories/*", CATEGORIES_ID);
        uriMatcher.addURI(AUTHORITY, "category/*", CATEGORY_ID);
    }
}
