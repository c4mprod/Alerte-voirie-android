package com.fabernovel.alertevoirie.entities;

import android.net.Uri;

import com.fabernovel.alertevoirie.data.CategoryProvider;

public interface Category {
    public static final Uri CONTENT_URI = Uri.parse("content://"+CategoryProvider.AUTHORITY+"/category");
    public static final Uri CHILDREN_CONTENT_URI = Uri.parse("content://"+CategoryProvider.AUTHORITY+"/categories");
    public static final String NAME = "name";
    public static final String CHILDREN = "children_id";
    public static final String PARENT = "parent_id";
    
    
}
