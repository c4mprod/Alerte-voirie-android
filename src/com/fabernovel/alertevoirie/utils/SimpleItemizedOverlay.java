package com.fabernovel.alertevoirie.utils;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.fabernovel.alertevoirie.R;
import com.fabernovel.alertevoirie.ReportDetailsActivity;
import com.fabernovel.alertevoirie.entities.Incident;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.MapView.LayoutParams;

@SuppressWarnings("rawtypes")
public class SimpleItemizedOverlay extends ItemizedOverlay {
    private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
    private View                   mBubbleView;
    private Context                c;
    private String                 name, address, incident;
    private MapView                mMapView;
    private boolean                clickable;

    public SimpleItemizedOverlay(Drawable defaultMarker, Context context, Incident incident, MapView mapview) {
        super(boundCenterBottom(defaultMarker));
        c = context;
        if (incident != null) {
            this.name = incident.description;
            this.address = incident.address;
            this.incident = incident.toString();
            this.clickable = (incident.invalidations > 0 || incident.state == 'R');
        }
        this.mMapView = mapview;

    }

    public void addOverlayItem(OverlayItem overlay) {
        mOverlays.add(overlay);
        populate();
    }

    @Override
    protected OverlayItem createItem(int i) {
        return mOverlays.get(i);
    }

    @Override
    public int size() {
        return mOverlays.size();
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, false);
    }

    @Override
    protected boolean onTap(int index) {
        if (incident != null) {
            OverlayItem tapped = getItem(index);
            MapView.LayoutParams params = new MapView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, tapped.getPoint(),
                                                                   LayoutParams.BOTTOM_CENTER);
            params.mode = MapView.LayoutParams.MODE_MAP;
            if (mBubbleView == null) {
                mBubbleView = ((LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.view_map_bubble, null);
                if (clickable) {
                    mBubbleView.findViewById(R.id.Bubble_arrow).setVisibility(View.VISIBLE);
                    mBubbleView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            Intent i = new Intent(c, ReportDetailsActivity.class);
                            i.putExtra("existing", true);
                            i.putExtra("event", (String) mBubbleView.getTag());
                            c.startActivity(i);

                        }
                    });
                } else {
                    mBubbleView.findViewById(R.id.Bubble_arrow).setVisibility(View.GONE);
                }
            }
            TextView title = (TextView) mBubbleView.findViewById(R.id.TextView_title);
            TextView subtitle = (TextView) mBubbleView.findViewById(R.id.TextView_subtitle);
            title.setText(name);
            subtitle.setText(address);
            mBubbleView.setTag(incident);
            mMapView.addView(mBubbleView, params);
            mMapView.getController().animateTo(tapped.getPoint());
            return true;
        }
        return super.onTap(index);
    }

    @Override
    public boolean onTap(GeoPoint p, MapView mapView) {
        mMapView.removeAllViews();// View(mBubbleView);
        return super.onTap(p, mapView);
    }
}
