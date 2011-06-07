/**
 * This file is part of the Alerte Voirie project.
 * 
 * Copyright (C) 2010-2011 C4M PROD
 * 
 * Alerte Voirie is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alerte Voirie is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Alerte Voirie.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.fabernovel.alertevoirie.utils;

import org.json.JSONArray;
import org.json.JSONException;

import android.database.AbstractCursor;

public class JSONCursor extends AbstractCursor {
    private String[] columns;
    private JSONArray dataArray;

    public JSONCursor(JSONArray data, String[] cols) {
        dataArray = data;
        columns = cols;
    }

    @Override
    public String[] getColumnNames() {
        return columns;
    }

    @Override
    public int getCount() {
        return dataArray.length();
    }

    @Override
    public double getDouble(int column) {
        try {
            return dataArray.getJSONObject(mPos).getDouble(getColumnName(column));
        } catch (JSONException e) {
            return 0;
        }
    }

    @Override
    public float getFloat(int column) {
        return (float) getDouble(column);
    }

    @Override
    public int getInt(int column) {
        try {
            return dataArray.getJSONObject(mPos).getInt(getColumnName(column));
        } catch (JSONException e) {
            return 0;
        }
    }

    @Override
    public long getLong(int column) {
        try {
            return dataArray.getJSONObject(mPos).getLong(getColumnName(column));
        } catch (JSONException e) {
            return 0;
        }
    }

    @Override
    public short getShort(int column) {
        return (short) getInt(column);
    }

    @Override
    public String getString(int column) {
        try {
            return dataArray.getJSONObject(mPos).getString(getColumnName(column));
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public boolean isNull(int column) {
        try {
            return dataArray.getJSONObject(mPos).isNull(getColumnName(column));
        } catch (JSONException e) {
            return true;
        }
    }
}