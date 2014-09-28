package de.happycarl.geotown.app.api.requests;

import android.util.Log;

import com.path.android.jobqueue.Params;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.TrackStartedEvent;

/**
 * Created by ole on 28.09.14.
 */
public class StartTrackRequest extends NetworkRequestJob {

    long routeId;

    public StartTrackRequest(long routeId) {
        super(new Params(3).requireNetwork().persist().groupBy("start-track"));
        this.routeId = routeId;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

        final long trackId = GeotownApplication.getGeotown().tracks().startTrack(routeId).execute().getId();
        Log.d("StartTrackRequest", "Got track id: " + trackId);

        GeotownApplication.mHandler.post(new Runnable() {
            @Override
            public void run() {
                GeotownApplication.getEventBus().post(new TrackStartedEvent(trackId));
            }
        });

    }

    @Override
    protected void onCancel() {

    }
}
