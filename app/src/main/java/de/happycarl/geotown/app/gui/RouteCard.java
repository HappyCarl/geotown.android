package de.happycarl.geotown.app.gui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.afollestad.cardsui.Card;
import com.afollestad.cardsui.CardAdapter;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by ole on 19.06.14.
 */
public class RouteCard extends Card implements Target {

    Context con;
    CardAdapter adapter;


    public RouteCard(Context context, CardAdapter adapter, String title, String content) {
        super(title,content);
        con = context;
        this.adapter = adapter;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
        Log.d("Picasso", "Loaded Bitmap");
        setThumbnail(con, bitmap);
        adapter.update(this, true);

    }

    @Override
    public void onBitmapFailed(Drawable drawable) {

    }

    @Override
    public void onPrepareLoad(Drawable drawable) {

    }
}
