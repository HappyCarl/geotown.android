package de.happycarl.geotown.app.gui.views;

import android.content.Context;
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

    GeoTownRoute geoTownRoute;

    ProgressBar progress;
    TextView progressText;
    Context c;

    int waypointCount = -1, finishedCount = -1;

    public ProgressCard(Context c, CardAdapter cardAdapter, GeoTownRoute r) {
        super(c,cardAdapter,r);
        geoTownRoute = r;
        this.c = c;

    }

    public int getLayout() {
        // Replace with your layout
        return R.layout.card_progress;

    }

    public void setProgressBar(ProgressBar progressBar) {
        progress = progressBar;
        if(geoTownRoute != null) {
            progress.setMax(getWaypointCount()+1);
            progress.setProgress(getFinishedWaypointCount()+1);
        }
    }

    public void setProgressText(TextView text) {
        progressText = text;
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
