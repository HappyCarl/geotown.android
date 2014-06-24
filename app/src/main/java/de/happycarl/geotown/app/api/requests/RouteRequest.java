package de.happycarl.geotown.app.api.requests;

import com.appspot.drive_log.geotown.model.Route;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.RouteDataReceivedEvent;

/**
 * Created by ole on 19.06.14.
 */
public class RouteRequest extends Job { //AsyncTask<Long, Void, Route> {
    private long routeId;

    public RouteRequest(long id) {
        super(new Params(3).requireNetwork().groupBy("fetch-route"));

        this.routeId = id;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        Route route = GeotownApplication.getGeotown().routes().get(routeId).execute();

        GeotownApplication.getEventBus().post(new RouteDataReceivedEvent(route));

    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }
}
