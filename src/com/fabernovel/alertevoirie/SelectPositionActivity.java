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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

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

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.layout_select_position);

        // init button
        findViewById(R.id.Button_validate).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = ((TextView) findViewById(R.id.EditText_address_number)).getText().toString()
                                 + ((TextView) findViewById(R.id.TextView_address)).getText().toString();
                Intent result = new Intent();
                result.putExtra(IntentData.EXTRA_ADDRESS, address);
                result.putExtra(IntentData.EXTRA_LONGITUDE, (double) currentPoint.getLongitudeE6()/1E6);
                result.putExtra(IntentData.EXTRA_LATITUDE, (double) currentPoint.getLatitudeE6()/1E6);
                setResult(RESULT_OK, result);
                finish();
            }
        });

        map = (MapView) findViewById(R.id.MapView_map);
        map.setBuiltInZoomControls(true);
        map.getController().setZoom(18);

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
        CursorOveray cursor = new CursorOveray(getResources().getDrawable(R.drawable.map_cursor));
        cursor.addOverlayItem(new OverlayItem(newGeo, null, null));
        map.getOverlays().add(cursor);
        map.invalidate();
        currentPoint = newGeo;

        ((TextView) findViewById(R.id.TextView_address)).setText(null);
        ((TextView) findViewById(R.id.EditText_address_number)).setText(null);
        new AddressGetter().execute((double) newGeo.getLatitudeE6() / 1E6, (double) newGeo.getLongitudeE6() / 1E6);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private class CursorOveray extends SimpleItemizedOverlay {

        public CursorOveray(Drawable defaultMarker) {
            super(defaultMarker);
        }

        @Override
        public boolean onTap(GeoPoint p, MapView mapView) {
            locationManager.removeUpdates(SelectPositionActivity.this);
            setMarker(p);
            return true;
        }

    }

    private class AddressGetter extends AsyncTask<Double, Void, String[]> {
        @Override
        protected String[] doInBackground(Double... params) {
            Geocoder geo = new Geocoder(SelectPositionActivity.this);
            String[] result = new String[2];
            try {
                List<Address> addr = geo.getFromLocation(params[0], params[1], 1);
                if (addr.size() > 0) {
                    result[0] = addr.get(0).getAddressLine(0);

                    Pattern p = Pattern.compile("^[\\d\\-]+");
                    Matcher m = p.matcher(result[0]);
                    if (m.find()) {
                        result[1] = m.group();
                        result[0] = result[0].replace(result[1], "");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result[0] != null) {
                ((TextView) findViewById(R.id.TextView_address)).setText(result[0]);
            }

            if (result[1] != null) {
                ((TextView) findViewById(R.id.EditText_address_number)).setText(result[1]);
            }

        }

    }

}
