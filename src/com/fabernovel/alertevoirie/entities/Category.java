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
