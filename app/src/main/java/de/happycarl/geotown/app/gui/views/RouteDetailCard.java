package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

import com.afollestad.cardsui.Card;
import com.afollestad.cardsui.CardAdapter;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.ViewById;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.models.GeoTownRoute;

/**
 * Created by jhbruhn on 22.06.14.
 */
public class RouteDetailCard extends Card {
    private static final Map<Location, String> CACHE = new HashMap<>();

    private TextView mLocationTextView;
    private TextView mOwnerTextView;
    private TextView mWaypointAmountTextView;

    private final GeoTownRoute mRoute;
    private final CardAdapter mAdapter;
    private String mLocationString;

    public RouteDetailCard(Context ctx, CardAdapter cardAdapter, GeoTownRoute route) {
        super("");
        this.mAdapter = cardAdapter;
        this.mRoute = route;
        this.mAdapter.update(this, true);
        new GeoCodingAsyncTask(ctx, new Location(mRoute.latitude, mRoute.longitude)).execute();

    }

    public void updateView(View view) {
        mLocationTextView = (TextView) view.findViewById(R.id.card_route_detail_location_text);
        mOwnerTextView = (TextView) view.findViewById(R.id.card_route_detail_owner_text);
        mWaypointAmountTextView = (TextView) view.findViewById(R.id.card_route_detail_waypoint_amount);
        updateUi();
    }

    private void updateUi() {
        if (mLocationTextView != null) {
            mLocationTextView.setText(mLocationString);
        }
        if (mOwnerTextView != null) {
            mOwnerTextView.setText("by " + mRoute.owner);
        }
        if (mWaypointAmountTextView != null) {
            mWaypointAmountTextView.setText(mRoute.waypoints().size() + "");
        }
        mAdapter.update(this, true);
    }

    public int getLayout() {
        return R.layout.card_route_detail;
    }

    private class Location {
        final double lat;
        final double lng;

        public Location(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        @Override
        public int hashCode() {
            return (int) (lat * lng * 42);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Location)) return false;

            Location that = (Location) o;
            return this.lat == that.lat && this.lng == that.lng;
        }
    }

    private class GeoCodingAsyncTask extends AsyncTask<Void, Void, String> {

        private final Location l;
        private final Context ctx;

        public GeoCodingAsyncTask(Context ctx, Location l) {
            this.l = l;
            this.ctx = ctx;
        }

        @Override
        protected String doInBackground(Void... locations) {
            if (CACHE.containsKey(l)) return CACHE.get(l);
            String result = ctx.getResources().getString(R.string.unknown_place);

            Geocoder gc = new Geocoder(ctx);

            try {
                Address address = gc.getFromLocation(this.l.lat, this.l.lng, 1).get(0);

                String town = ctx.getResources().getString(R.string.unknown_town);
                if (address.getLocality() != null) {
                    town = address.getLocality();
                }
                String country = ctx.getResources().getString(R.string.unknown_country);
                if (address.getCountryName() != null) {
                    country = address.getCountryName();
                }
                result = town + ", " + country;
            } catch (IOException | IndexOutOfBoundsException e) {
                e.printStackTrace();
            }

            CACHE.put(l, result);

            return result;
        }

        @Override
        protected void onPostExecute(String location) {
            mLocationString = location;
            updateUi();
        }


    }
}
