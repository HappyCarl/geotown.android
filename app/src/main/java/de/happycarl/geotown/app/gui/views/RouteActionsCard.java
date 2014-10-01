package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;

import com.afollestad.cardsui.Card;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
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

    @InjectView(R.id.card_route_actions_play_button)
    public ImageButton mPlayButton;

    @InjectView(R.id.card_route_actions_star_checkbox)
    CheckBox mCheckBox;

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
        ButterKnife.inject(this, view);
        updateUI();
    }

    @OnClick(R.id.card_route_actions_star_checkbox)
    void onStarCheckboxClicked() {
        mListener.onCheckBoxClicked(mCheckBox.isChecked());
    }

    @OnClick(R.id.card_route_actions_play_button)
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
