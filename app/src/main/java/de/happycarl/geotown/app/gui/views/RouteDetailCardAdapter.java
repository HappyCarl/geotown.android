package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.view.View;

import com.afollestad.cardsui.CardAdapter;
import com.afollestad.cardsui.CardBase;

import de.happycarl.geotown.app.R;

/**
 * Created by jhbruhn on 22.06.14.
 */
public class RouteDetailCardAdapter extends CardAdapter {

    public RouteDetailCardAdapter(Context context, int accentColorRes) {
        super(context, accentColorRes);

        registerLayout(R.layout.card_loading);
        registerLayout(R.layout.card_route_detail);
        registerLayout(R.layout.card_route_actions);
    }

    @Override
    public View onViewCreated(int index, View recycled, CardBase item) {
        View view = super.onViewCreated(index, recycled, item);
        if (item instanceof RouteDetailCard) {
            ((RouteDetailCard) item).updateView(view);
        } else if (item instanceof RouteActionsCard) {
            ((RouteActionsCard) item).buildView(view);
        }
        return view;
    }


}
