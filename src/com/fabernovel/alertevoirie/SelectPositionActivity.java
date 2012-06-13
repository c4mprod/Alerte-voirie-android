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
package com.fabernovel.alertevoirie;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.TextView;

import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.IntentData;
import com.fabernovel.alertevoirie.utils.LocationHelper;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class SelectPositionActivity extends MapActivity implements LocationListener {
    private Location        currentBestLocation = null;
    private MapView         map;
    private LocationManager locationManager;
    private GeoPoint        currentPoint;
    private Geocoder        geo;
    private boolean         search              = false;

    private CursorOveray    cursorOverlay;
    private AsyncTask<String, Void, Address> adressTask;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.layout_select_position);

        // init button
        findViewById(R.id.Button_validate).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (search) {
                    new AddressGetter().execute();
                } else {

                    String address = (((TextView) findViewById(R.id.EditText_address_number)).getText().toString() + " "
                                      + ((TextView) findViewById(R.id.EditText_address_street)).getText().toString() + " \n"
                                      + ((TextView) findViewById(R.id.EditText_address_postcode)).getText().toString() + " " + ((TextView) findViewById(R.id.EditText_address_town)).getText()
                                                                                                                                                                                    .toString()).trim();
                    Intent result = new Intent();
                    result.putExtra(IntentData.EXTRA_ADDRESS, address);
                    result.putExtra(IntentData.EXTRA_LONGITUDE, (double) currentPoint.getLongitudeE6() / 1E6);
                    result.putExtra(IntentData.EXTRA_LATITUDE, (double) currentPoint.getLatitudeE6() / 1E6);
                    setResult(RESULT_OK, result);
                    finish();
                }
            }
        });

        OnFocusChangeListener ofc = new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) enableSearch();

            }
        };

        ((Button) findViewById(R.id.ButtonMapPosition)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!("-").equals(((Button) v).getTag())) {
                    ((Button) v).setTag("-");
                    ((Button) v).setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_bg_diminuer));
                    SelectPositionActivity.this.findViewById(R.id.TextViewpos02).setVisibility(View.GONE);
                    SelectPositionActivity.this.findViewById(R.id.Layoutpos01).setVisibility(View.GONE);
                    SelectPositionActivity.this.findViewById(R.id.TextViewpos03).setVisibility(View.GONE);
                } else {
                    ((Button) v).setTag("+");
                    ((Button) v).setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_bg_agrandir));
                    SelectPositionActivity.this.findViewById(R.id.TextViewpos02).setVisibility(View.VISIBLE);
                    SelectPositionActivity.this.findViewById(R.id.TextViewpos03).setVisibility(View.VISIBLE);
                    SelectPositionActivity.this.findViewById(R.id.Layoutpos01).setVisibility(View.VISIBLE);
                }

            }
        });

        ((TextView) findViewById(R.id.EditText_address_number)).setOnFocusChangeListener(ofc);
        ((TextView) findViewById(R.id.EditText_address_street)).setOnFocusChangeListener(ofc);
        ((TextView) findViewById(R.id.EditText_address_postcode)).setOnFocusChangeListener(ofc);
        ((TextView) findViewById(R.id.EditText_address_town)).setOnFocusChangeListener(ofc);

        geo = new Geocoder(this);

        map = (MapView) findViewById(R.id.MapView_map);
        map.setBuiltInZoomControls(true);
        map.getController().setZoom(18);
        map.setSatellite(true);

        findViewById(R.id.Button_validate).setEnabled(false);

        // get edit info
        String oldAddress = getIntent().getStringExtra(IntentData.EXTRA_ADDRESS);
        boolean edit = oldAddress != null;
        if (edit) {
            adressTask = new AsyncTask<String, Void, Address>() {
                @Override
                protected Address doInBackground(String... params) {
                    try {
                        List<Address> results = geo.getFromLocationName(params[0], 1);
                        if (results.size() > 0) {
                            return results.get(0);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                protected void onPostExecute(Address result) {
                    ((TextView) findViewById(R.id.EditText_address_street)).setText(result.getAddressLine(0));
                    ((TextView) findViewById(R.id.EditText_address_postcode)).setText(result.getPostalCode());
                    ((TextView) findViewById(R.id.EditText_address_town)).setText(result.getLocality());
                    cursorOverlay = new CursorOveray(getResources().getDrawable(R.drawable.map_cursor));
                    GeoPoint oldGeo = new GeoPoint((int) (result.getLatitude() * 1E6), (int) (result.getLongitude() * 1E6));
                    cursorOverlay.setGeopoint(oldGeo);
                    map.getOverlays().add(cursorOverlay);
                    map.getController().animateTo(oldGeo);
                };
            };
            adressTask.execute(oldAddress);
        } else {
            // Acquire a reference to the system Location Manager
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            currentBestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            // Register the listener with the Location Manager to receive location
            // updates
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

            cursorOverlay = new CursorOveray(getResources().getDrawable(R.drawable.map_cursor));
            map.getOverlays().add(cursorOverlay);
            map.invalidate();
        }
        if (currentBestLocation != null) {
            handleNewLocation(currentBestLocation);
        }
    }

    protected void enableSearch() {
        if (findViewById(R.id.Layoutpos01).getVisibility() == View.VISIBLE) {
            ((Button) findViewById(R.id.Button_validate)).setText(R.string.address_search);
            findViewById(R.id.Button_validate).setEnabled(true);
            search = true;
        }
    }

    public void onLocationChanged(Location location) {
        // Called when a new location is found by the location provider.
        if (LocationHelper.isBetterLocation(location, currentBestLocation)) {
            handleNewLocation(location);
        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    private void handleNewLocation(Location location) {
        currentBestLocation = location;
        GeoPoint newGeo = LocationHelper.geoFromLocation(location);
        map.getController().animateTo(newGeo);
        setMarker(newGeo);
    }

    private void setMarker(GeoPoint newGeo) {

        cursorOverlay.setGeopoint(newGeo);
        currentPoint = newGeo;

        // ((TextView) findViewById(R.id.TextView_address)).setText(null);
        ((TextView) findViewById(R.id.EditText_address_number)).setText(null);
        Log.d(Constants.PROJECT_TAG, "Position: " + (double) newGeo.getLatitudeE6() / 1E6 + " / " + (double) newGeo.getLongitudeE6() / 1E6);
        if (!search) new AddressGetter().execute((double) newGeo.getLatitudeE6() / 1E6, (double) newGeo.getLongitudeE6() / 1E6);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private class CursorOveray extends ItemizedOverlay<OverlayItem> {
        GeoPoint p;

        public CursorOveray(Drawable defaultMarker) {
            super(boundCenterBottom(defaultMarker));
            // TODO Auto-generated constructor stub
        }

        public void setGeopoint(GeoPoint geo) {
            p = geo;
            populate();
        }

        @Override
        protected OverlayItem createItem(int i) {
            OverlayItem item = new OverlayItem(p, "", "");
            return item;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean onTap(GeoPoint p, MapView mapView) {
            if (locationManager != null) {
                locationManager.removeUpdates(SelectPositionActivity.this);
            }
            setMarker(p);
            return true;
        }

    }

    private class AddressGetter extends AsyncTask<Double, Void, String[]> {

        protected GeoPoint geopoint;

        @Override
        protected String[] doInBackground(Double... params) {

            String[] result = new String[4];
            try {
                List<Address> addr;

                if (!search) {
                    addr = geo.getFromLocation(params[0], params[1], 1);
                } else {
                    String address = ((TextView) findViewById(R.id.EditText_address_number)).getText().toString() + " "
                                     + ((TextView) findViewById(R.id.EditText_address_street)).getText().toString() + " , "
                                     + ((TextView) findViewById(R.id.EditText_address_town)).getText().toString();
                    addr = geo.getFromLocationName(address, 1);

                    if (addr.size() > 0) {
                        geopoint = new GeoPoint((int) (addr.get(0).getLatitude() * 1E6), (int) (addr.get(0).getLongitude() * 1E6));

                    }
                }

                if (addr.size() > 0) {
                    result[0] = addr.get(0).getAddressLine(0);
                    result[3] = addr.get(0).getLocality();
                    result[2] = addr.get(0).getPostalCode();

                    Pattern p = Pattern.compile("^[\\d\\-]+");
                    Matcher m = p.matcher(result[0]);
                    if (m.find()) {
                        result[1] = m.group();
                        result[0] = result[0].replace(result[1], "");
                    }
                }
            } catch (IOException e) {
                Log.e(Constants.PROJECT_TAG, "Address error", e);
            }
            return result;
        }

        @Override
        protected void onPostExecute(String[] result) {

            if (geopoint != null) {
                setMarker(geopoint);
                map.getController().animateTo(geopoint);
                geopoint = null;
            }

            Log.d(Constants.PROJECT_TAG, "n° : " + result[0]);
            Log.d(Constants.PROJECT_TAG, "Rue : " + result[1]);
            Log.d(Constants.PROJECT_TAG, "CP : " + result[2]);
            Log.d(Constants.PROJECT_TAG, "Ville : " + result[3]);

            String number = "", street = "", postcode = "", town = "";

            if (result[2] != null) {

                number = result[2].equals(result[1]) ? "" : result[1];
                street = (result[3]).equals(new String(result[0]).trim()) ? "" : result[0].trim();
                postcode = result[2];
                town = result[3];

            } else {
                for (String string : result) {
                    street += (string != null ? " " + string : "");
                }
                street = street.trim();

            }

            ((TextView) findViewById(R.id.EditText_address_street)).setText(street != null ? street : "");
            ((TextView) findViewById(R.id.EditText_address_number)).setText(number != null ? number : "");
            ((TextView) findViewById(R.id.EditText_address_postcode)).setText(postcode);
            ((TextView) findViewById(R.id.EditText_address_town)).setText(town);

            if (!((TextView) findViewById(R.id.EditText_address_street)).getText().toString().equals("")
                && !((TextView) findViewById(R.id.EditText_address_postcode)).getText().toString().equals("")
                && !((TextView) findViewById(R.id.EditText_address_town)).getText().toString().equals("")) {
                findViewById(R.id.Button_validate).setEnabled(true);
            } else
                findViewById(R.id.Button_validate).setEnabled(false);

            ((Button) findViewById(R.id.Button_validate)).setText(R.string.select_position_btn_validate);
            search = false;

            Log.d(Constants.PROJECT_TAG, "Number : " + number);
            Log.d(Constants.PROJECT_TAG, "Street : " + street);
            Log.d(Constants.PROJECT_TAG, "Postcode : " + postcode);
            Log.d(Constants.PROJECT_TAG, "Town : " + town);
        }
    }
}
