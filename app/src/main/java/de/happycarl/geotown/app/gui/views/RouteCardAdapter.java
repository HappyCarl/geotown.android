package de.happycarl.geotown.app.gui.views;

import android.content.Context;

import com.afollestad.cardsui.CardAdapter;

import de.happycarl.geotown.app.R;

/**
 * Created by jhbruhn on 22.06.14.
 */
public class RouteCardAdapter extends CardAdapter {
    public RouteCardAdapter(Context context, int accentColorRes) {
        super(context, accentColorRes);

        registerLayout(R.layout.card_loading);
    }
}
