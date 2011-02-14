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
import com.fabernovel.alertevoirie.entities.Incident;
import com.fabernovel.alertevoirie.entities.IntentData;
import com.fabernovel.alertevoirie.utils.LocationHelper;
import com.fabernovel.alertevoirie.utils.SimpleItemizedOverlay;
import com.google.android.maps.GeoPoint;
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

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        currentBestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        // Register the listener with the Location Manager to receive location
        // updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

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
        locationManager.removeUpdates(this);
    }

    private void handleNewLocation(Location location) {
        currentBestLocation = location;
        GeoPoint newGeo = LocationHelper.geoFromLocation(location);
        map.getController().animateTo(newGeo);
        setMarker(newGeo);
    }

    private void setMarker(GeoPoint newGeo) {

        map.getOverlays().clear();
        CursorOveray cursor = new CursorOveray(getResources().getDrawable(R.drawable.map_cursor), this, null, map);
        cursor.addOverlayItem(new OverlayItem(newGeo, null, null));
        map.getOverlays().add(cursor);
        map.invalidate();
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

    private class CursorOveray extends SimpleItemizedOverlay {

        public CursorOveray(Drawable defaultMarker, Context c, Incident i, MapView map) {
            super(defaultMarker, c, i, map);
        }

        @Override
        public boolean onTap(GeoPoint p, MapView mapView) {
            locationManager.removeUpdates(SelectPositionActivity.this);
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
