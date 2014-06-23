package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;

import com.afollestad.cardsui.Card;
import com.appspot.drive_log.geotown.model.Route;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;

/**
 * Created by jhbruhn on 22.06.14.
 */
public class RouteActionsCard extends Card {

    public interface RouteActionsCardListener {
        public void onCheckBoxClicked(boolean status);

        public void onPlayButtonClicked();
    }

    @InjectView(R.id.card_route_actions_play_button)
    ImageButton mPlayButton;

    @InjectView(R.id.card_route_actions_star_checkbox)
    CheckBox mCheckBox;

    private Route mRoute;

    private boolean mSaved = false;

    private RouteActionsCardListener mListener;

    public RouteActionsCard(Context ctx, RouteActionsCardListener l, Route route) {
        super("");
        this.mListener = mListener;
        this.mRoute = route;
    }

    @Override
    public int getLayout() {
        return R.layout.card_route_actions;
    }

    public void buildView(View view) {
        ButterKnife.inject(this, view);
    }

    public boolean isSaved() {
        return mSaved;
    }

    public void setSaved(boolean mSaved) {
        this.mSaved = mSaved;
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
        if (GeotownApplication.getPreferences().getLong(AppConstants.PREF_CURRENT_ROUTE, 0L) == mRoute.getId()) {
            this.mPlayButton.setImageResource(R.drawable.ic_play);
        } else {
            this.mPlayButton.setImageResource(R.drawable.ic_play_inactive);
        }
        mCheckBox.setChecked(mSaved);

    }
}
