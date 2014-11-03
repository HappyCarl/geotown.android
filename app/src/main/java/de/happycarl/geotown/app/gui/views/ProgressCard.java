package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.cardsui.CardAdapter;

import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.models.GeoTownRoute;
import de.happycarl.geotown.app.models.GeoTownWaypoint;

/**
 * Created by ole on 22.06.14.
 */
public class ProgressCard extends RouteCard {

    final GeoTownRoute geoTownRoute;

    private ProgressBar progress;
    private TextView progressText;

    final Context c;
    View view;


    int waypointCount = -1, finishedCount = -1;

    public ProgressCard(Context c, CardAdapter adapter, GeoTownRoute r) {
        super(c, adapter, r);
        geoTownRoute = r;
        this.c = c;

    }

    @Override
    public int getLayout() {
        // Replace with your toolbar
        return R.layout.card_progress;

    }

    public void updateView(View view) {
        this.view = view;
        progress = (ProgressBar) view.findViewById(R.id.progress);
        progressText = (TextView) view.findViewById(R.id.progress_text);
        updateUI();
    }

    public void updateUI() {
        setProgressBar();
        setProgressText();
    }

    private void setProgressBar() {
        if(progress == null) return;
        if(geoTownRoute != null) {
            progress.setMax(getWaypointCount()+1);
            progress.setProgress(getFinishedWaypointCount()+1);
        }
    }

    private void setProgressText() {
        if(progressText == null) return;
        progressText.setText(getFinishedWaypointCount() + "/" + getWaypointCount() + " " + c.getResources().getString(R.string.waypoints));
    }

    private int getWaypointCount() {
        if(waypointCount == -1) {
            waypointCount = geoTownRoute.waypoints().size();
        }
        return waypointCount;
    }
    private int getFinishedWaypointCount() {
        if (finishedCount == -1) {
            finishedCount = 0;
            for(GeoTownWaypoint w : geoTownRoute.waypoints()) {
                if(w.done)
                    finishedCount++;
            }
        }
        return finishedCount;
    }



}
