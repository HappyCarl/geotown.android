package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

import com.afollestad.cardsui.Card;
import com.afollestad.cardsui.CardAdapter;
import com.appspot.drive_log.geotown.model.Route;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.happycarl.geotown.app.R;

/**
 * Created by jhbruhn on 22.06.14.
 */
public class RouteDetailCard extends Card {
    private static final Map<Location, String> CACHE = new HashMap<>();
    @InjectView(R.id.card_route_detail_location_text)
    TextView mLocationTextView;
    @InjectView(R.id.card_route_detail_owner_text)
    TextView mOwnerTextView;
    @InjectView(R.id.card_route_detail_waypoint_amount)
    TextView mWaypointAmountTextView;
    private Route mRoute;
    private Context mContext;
    private CardAdapter mAdapter;
    private int mWaypointAmount = 0;
    private Location mLocation;
    private String mLocationString;

    public RouteDetailCard(Context ctx, CardAdapter cardAdapter, Route route) {
        super("");
        this.mAdapter = cardAdapter;
        this.mContext = ctx;
        this.mRoute = route;
        this.mLocation = new Location(mRoute.getLatitude(), mRoute.getLongitude());
        this.mAdapter.update(this, true);
        new GeoCodingAsyncTask(this.mContext, mLocation).execute();

    }

    public void setWaypointAmount(int i) {

        mWaypointAmount = i;
        this.mWaypointAmountTextView.post(new Runnable() {
            @Override
            public void run() {
                updateUi();
            }
        });
    }

    public void updateView(View view) {
        ButterKnife.inject(this, view);

        updateUi();
    }

    private void updateUi() {
        if (mLocationTextView != null) {
            mLocationTextView.setText(mLocationString);
        }
        if (mOwnerTextView != null) {
            mOwnerTextView.setText("by " + mRoute.getOwner().getUsername());
        }
        if (mWaypointAmountTextView != null) {
            mWaypointAmountTextView.setText("" + mWaypointAmount);
        }
        mAdapter.update(this, true);
    }

    public int getLayout() {
        return R.layout.card_route_detail;
    }

    private class Location {
        double lat, lng;

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

        private Location l;
        private Context ctx;

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
