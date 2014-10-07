package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.afollestad.cardsui.Card;
import com.afollestad.cardsui.CardAdapter;

import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.models.GeoTownRoute;

/**
 * Created by jhbruhn on 22.06.14.
 */
public class RouteDetailCard extends Card {
    private TextView mLocationTextView;
    private TextView mOwnerTextView;
    private TextView mWaypointAmountTextView;

    private final GeoTownRoute mRoute;
    private final CardAdapter mAdapter;

    public RouteDetailCard(Context ctx, CardAdapter cardAdapter, GeoTownRoute route) {
        super("");
        this.mAdapter = cardAdapter;
        this.mRoute = route;
        this.mAdapter.update(this, true);
    }

    public void updateView(View view) {
        mLocationTextView = (TextView) view.findViewById(R.id.card_route_detail_location_text);
        mOwnerTextView = (TextView) view.findViewById(R.id.card_route_detail_owner_text);
        mWaypointAmountTextView = (TextView) view.findViewById(R.id.card_route_detail_waypoint_amount);
        updateUi();
    }

    private void updateUi() {
        if (mLocationTextView != null) {
            mLocationTextView.setText(mRoute.location);
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
}
