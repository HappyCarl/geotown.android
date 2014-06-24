package de.happycarl.geotown.app.api.requests;

import com.appspot.drive_log.geotown.model.Route;
import com.appspot.drive_log.geotown.model.RouteCollection;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.ArrayList;
import java.util.List;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.NearRoutesDataReceivedEvent;

/**
 * Created by jhbruhn on 20.06.14.
 */
public class NearRoutesRequest extends Job {

    private double lat, lng, radius;

    public NearRoutesRequest(double lat, double lng, double radius) {
        super(new Params(3).requireNetwork().groupBy("fetch-near-routes"));
        this.lat = lat;
        this.lng = lng;
        this.radius = radius;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        RouteCollection rc = GeotownApplication.getGeotown().routes().listNear(lat, lng, radius).execute();

        List<Route> routes = new ArrayList<>();
        if (rc != null && rc.getItems() != null) {
            routes = rc.getItems();
        }

        GeotownApplication.getEventBus().post(new NearRoutesDataReceivedEvent(routes));
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }
}
