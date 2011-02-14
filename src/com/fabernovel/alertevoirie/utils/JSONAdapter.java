package com.fabernovel.alertevoirie.utils;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fabernovel.alertevoirie.entities.Constants;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class JSONAdapter extends BaseAdapter {
    protected static final int  TYPE_CATEGORY = 0;
    protected static final int  TYPE_ITEM     = 1;

    private JSONArray           data;
    private LayoutInflater      inflater;
    private int                 cellLayout;
    private String[]            from;
    private int[]               to;
    private String              jsonObjectName;
    private String              categoryField;
    private int                 categoryLayout;

    private SparseArray<String> categories;

    public JSONAdapter(Context context, JSONArray data, int cellLayout, String[] from, int[] to, String jsonObjectName) {
        this.data = data;
        this.cellLayout = cellLayout;
        this.from = from;
        this.to = to;
        this.jsonObjectName = jsonObjectName;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public JSONAdapter(Context context, JSONArray data, int cellLayout, String[] from, int[] to, String jsonObjectName, String categoryField, int categoryLayout) {
        this(context, data, cellLayout, from, to, jsonObjectName);

        this.categoryField = categoryField;
        this.categoryLayout = categoryLayout;

        String currentCat = null;

        categories = new SparseArray<String>(2);

        if (data != null) for (int i = 0, j = 0; i < data.length(); i++, j++) {
            String cat = getCategoryOfItem(i);
            Log.d(Constants.PROJECT_TAG, "catégorie : " + cat);
            if (!cat.equals(currentCat)) {
                Log.d(Constants.PROJECT_TAG, "category " + j + " : " + cat);
                categories.put(j++, cat);
                currentCat = cat;

            }
        }
    }

    public int getRealPositionOfItem(int position) {

        return position - (categoryForPosition(position) + 1);
    }

    protected String getCategoryOfItem(int itemId) {
        try {
            if (jsonObjectName != null) {
                return data.getJSONObject(itemId).getJSONObject(jsonObjectName).getString(categoryField);
            } else {
                return data.getJSONObject(itemId).getString(categoryField);
            }
        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "category error", e);
            return null;
        }
    }

    @Override
    public int getCount() {
        if (data != null) {
            Log.d(Constants.PROJECT_TAG, "get count = " + data.length());
            return data.length() + (categories == null ? 0 : categories.size());
        } else {
            Log.d(Constants.PROJECT_TAG, "zero !!!");
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        if (categories != null) {
            position -= (categoryForPosition(position) + 1);
        }
        try {
            if (jsonObjectName != null) {
                return data.getJSONObject(position).getJSONObject(jsonObjectName);
            } else {
                return data.get(position);
            }
        } catch (JSONException e) {
            Log.e(Constants.PROJECT_TAG, "get item exception", e);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (categories != null && categories.indexOfKey(position) >= 0) {
            Log.d(Constants.PROJECT_TAG, "type categorie");
            return TYPE_CATEGORY;
        } else {
            Log.d(Constants.PROJECT_TAG, "type item ");
            return TYPE_ITEM;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(Constants.PROJECT_TAG, "getview position : " + position);

        switch (getItemViewType(position)) {
            case TYPE_ITEM:
                Log.d(Constants.PROJECT_TAG, "get item number " + position);
                View v;
                View[] subViews;
                if (convertView != null && convertView.getTag() != null) {
                    v = convertView;
                    subViews = (View[]) v.getTag();
                } else {
                    v = inflater.inflate(cellLayout, null);
                    subViews = new View[to.length];
                    for (int i = 0; i < to.length; i++) {
                        subViews[i] = v.findViewById(to[i]);
                    }
                    v.setTag(subViews);
                }

                try {
                    for (int i = 0; i < subViews.length; i++) {
                        View subView = subViews[i];
                        if (subView instanceof TextView) {
                            String text;
                            text = ((JSONObject) getItem(position)).getString(from[i]);
                            ((TextView) subView).setText(text);
                        } else if (subView instanceof ImageView) {
                            // TODO download image
                        }
                    }
                } catch (Exception e) {
                    Log.e(Constants.PROJECT_TAG, "Error getting views", e);
                }
                return v;

            case TYPE_CATEGORY:
                Log.d(Constants.PROJECT_TAG, "getCat : " + position);
                TextView tv;
                if (convertView != null && convertView instanceof TextView) {
                    tv = (TextView) convertView;
                } else {
                    tv = (TextView) inflater.inflate(categoryLayout, null);
                }
                tv.setText(categories.get(position));

                return tv;

            default:
                return null;
        }
    }

    private int categoryForPosition(int position) {
        int i = 0;
        for (; i <= position && i < categories.size(); i++) {
            if (categories.keyAt(i) >= position) break;
        }
        Log.d(Constants.PROJECT_TAG, "category for position " + position + " = " + (i - 1));

        return i - 1;
    }
}
