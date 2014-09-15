package de.happycarl.geotown.app.api.requests;

import com.appspot.drive_log.geotown.model.Waypoint;
import com.appspot.drive_log.geotown.model.WaypointCollection;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.ArrayList;
import java.util.List;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.RouteWaypointsReceivedEvent;

/**
 * Created by ole on 20.06.14.
 */
public class GetRouteWaypointsRequest extends NetworkRequestJob {

    private final long routeId;

    public GetRouteWaypointsRequest(long routeId) {
        super(new Params(3).requireNetwork().groupBy("fetch-waypoints"));
        this.routeId = routeId;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        WaypointCollection wc = GeotownApplication.getGeotown().waypoints().list(routeId).execute();

        List<Waypoint> waypoints = new ArrayList<>();
        if (wc != null && wc.getItems() != null) {
            waypoints = wc.getItems();
        }

        final List<Waypoint> pedaB = waypoints;
        GeotownApplication.mHandler.post(new Runnable() {
            @Override
            public void run() {
                GeotownApplication.getEventBus().post(new RouteWaypointsReceivedEvent(pedaB));
            }
        });
    }

    @Override
    protected void onCancel() {

    }

}
