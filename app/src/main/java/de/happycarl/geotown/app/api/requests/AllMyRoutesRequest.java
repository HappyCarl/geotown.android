package de.happycarl.geotown.app.api.requests;

import com.appspot.drive_log.geotown.model.Route;
import com.appspot.drive_log.geotown.model.RouteCollection;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.MyRoutesDataReceivedEvent;

/**
 * Created by ole on 18.06.14.
 */
public class AllMyRoutesRequest extends Job {
    private static final AtomicInteger jobCounter = new AtomicInteger(0);

    private final int id;

    public AllMyRoutesRequest() {
        super(new Params(1).requireNetwork().groupBy("fetch-my-routes"));
        id = jobCounter.incrementAndGet();
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        if (id != jobCounter.get()) {
            return;
        }

        RouteCollection rc = null;
        rc = GeotownApplication.getGeotown().routes().listMine().execute();

        List<Route> routes = new ArrayList<>();
        if (rc != null && rc.getItems() != null) {
            routes = rc.getItems();
        }

        GeotownApplication.getEventBus().post(new MyRoutesDataReceivedEvent(routes));

    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }
}
