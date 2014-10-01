package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.afollestad.cardsui.Card;
import com.afollestad.cardsui.CardAdapter;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import de.happycarl.geotown.app.util.GoogleUtils;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.models.GeoTownRoute;

/**
 * Created by ole on 19.06.14.
 */
public class RouteCard extends Card implements Target {
    final Context con;
    final CardAdapter adapter;
    final long routeID;
    final String owner;
    final String location;

    private void updateContent() {
        this.setContent(location + " by " + owner);
    }

    public RouteCard(Context context, CardAdapter adapter, GeoTownRoute route) {
        super(route.name, "");

        con = context;
        this.adapter = adapter;

        routeID = route.id;
        owner = route.owner;
        location = route.location;
        updateContent();


        Picasso.with(context).load(GoogleUtils.getStaticMapUrl(route.latitude, route.longitude, 8, 128)).placeholder(R.drawable.ic_launcher).into(this);
    }

    public long getRouteID() {
        return routeID;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
        setThumbnail(con, bitmap);
        adapter.update(this, false);
    }

    @Override
    public void onBitmapFailed(Drawable drawable) {

    }

    @Override
    public void onPrepareLoad(Drawable drawable) {

    }
}
