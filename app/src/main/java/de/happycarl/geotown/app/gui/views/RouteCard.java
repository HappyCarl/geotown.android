package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.view.View;

import com.afollestad.cardsui.Card;
import com.afollestad.cardsui.CardAdapter;
import com.afollestad.cardsui.CardBase;
import com.appspot.drive_log.geotown.model.Route;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.models.GeoTownRoute;

/**
 * Created by ole on 19.06.14.
 */
public class RouteCard extends Card implements Target {
    private static final Map<Location, String> CACHE = new HashMap<>();

    Context con;
    CardAdapter adapter;
    long routeID;
    String owner;
    Location location;

    public RouteCard(Context context, CardAdapter adapter, Route route) {
        super(route.getName(), "");
        con = context;
        this.adapter = adapter;
        routeID = route.getId();
        owner = route.getOwner().getUsername();
        location = new Location(route.getLatitude(), route.getLongitude());
        updateContent();
        this.adapter.update(this, true);
        new GeoCodingAsyncTask(context, location).execute();
    }

    private void updateContent() {
        this.setContent(CACHE.get(location) + "\nby " + owner);
    }

    public RouteCard(Context context, CardAdapter adapter, GeoTownRoute route) {
        super(route.name, "");
        con = context;
        this.adapter = adapter;
        routeID = route.id;
        owner = route.owner;
        location = new Location(route.latitude, route.longitude);
        updateContent();
        this.adapter.update(this, true);
        new GeoCodingAsyncTask(context, location).execute();
    }

    public long getRouteID() {
        return routeID;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
        setThumbnail(con, bitmap);
        adapter.update(this, true);

    }


    @Override
    public void onBitmapFailed(Drawable drawable) {

    }

    @Override
    public void onPrepareLoad(Drawable drawable) {

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
            updateContent();
            adapter.update(RouteCard.this, true);
        }


    }
}
