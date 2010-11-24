package com.fabernovel.alertevoirie;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.fabernovel.alertevoirie.entities.Category;
import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.IntentData;

public class SelectCategoryActivity extends ListActivity {
    private static final String[] PROJECTION = new String[] { BaseColumns._ID, Category.NAME, Category.CHILDREN };

    private Cursor                categoryCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_select_category);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_nouveau_rapport);

        long categoryId = getIntent().getLongExtra(IntentData.EXTRA_CATEGORY_ID, 0);
        categoryCursor = managedQuery(ContentUris.withAppendedId(Category.CHILDREN_CONTENT_URI, categoryId), PROJECTION, null, null, null);
        setListAdapter(new SimpleCursorAdapter(this, R.layout.cell_text, categoryCursor, new String[] { Category.NAME }, new int[] { R.id.TextView_text }));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        categoryCursor.moveToPosition(position);

        Log.d(Constants.PROJECT_TAG, "setResult? " + categoryCursor.isNull(categoryCursor.getColumnIndex(Category.CHILDREN)));
        if (categoryCursor.isNull(categoryCursor.getColumnIndex(Category.CHILDREN))) {
            Intent result = new Intent();
            result.putExtra(IntentData.EXTRA_CATEGORY_ID, id);
            setResult(RESULT_OK, result);
            finish();
        } else {
            Intent i = new Intent(this, SelectCategoryActivity.class);
            i.putExtra(IntentData.EXTRA_CATEGORY_ID, id);
            startActivityForResult(i, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        }
    }
}