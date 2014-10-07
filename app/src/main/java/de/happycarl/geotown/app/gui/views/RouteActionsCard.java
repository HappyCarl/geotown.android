package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;

import com.afollestad.cardsui.Card;


import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EView;
import org.androidannotations.annotations.ViewById;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.models.GeoTownRoute;

/**
 * Created by jhbruhn on 22.06.14.
 */
public class RouteActionsCard extends Card {

    public interface RouteActionsCardListener {
        public void onCheckBoxClicked(boolean status);

        public void onPlayButtonClicked();
    }

    private ImageButton mPlayButton;
    private CheckBox mCheckBox;

    private final GeoTownRoute mRoute;

    private final RouteActionsCardListener mListener;

    public RouteActionsCard(Context ctx, RouteActionsCardListener l, GeoTownRoute route) {
        super("");
        this.mListener = l;
        this.mRoute = route;
    }

    @Override
    public int getLayout() {
        return R.layout.card_route_actions;
    }

    public void buildView(View view) {
        mPlayButton = (ImageButton) view.findViewById(R.id.card_route_actions_play_button);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlayButtonClicked();
            }
        });
        mCheckBox = (CheckBox) view.findViewById(R.id.card_route_actions_star_checkbox);
        mCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStarCheckboxClicked();
            }
        });
        updateUI();
    }

    void onStarCheckboxClicked() {
        mListener.onCheckBoxClicked(mCheckBox.isChecked());
    }

    void onPlayButtonClicked() {
        mListener.onPlayButtonClicked();
    }

    private void updateUI() {
        if(this.mPlayButton == null) return;
        if (GeotownApplication.getPreferences().getLong(AppConstants.PREF_CURRENT_ROUTE, 0L) == mRoute.id) {
            this.mPlayButton.setImageResource(R.drawable.ic_play);
        } else {
            this.mPlayButton.setImageResource(R.drawable.ic_play_inactive);
        }
        mCheckBox.setChecked(mRoute.starred);

    }
}
