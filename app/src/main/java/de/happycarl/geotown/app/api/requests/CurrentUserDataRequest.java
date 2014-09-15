package de.happycarl.geotown.app.api.requests;

import com.appspot.drive_log.geotown.model.UserData;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.concurrent.atomic.AtomicInteger;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.CurrentUserDataReceivedEvent;

/**
 * Created by ole on 19.06.14.
 */
public class CurrentUserDataRequest extends NetworkRequestJob {
    private static final AtomicInteger jobCounter = new AtomicInteger(0);

    private final int id;

    public CurrentUserDataRequest() {
        super(new Params(1).requireNetwork().groupBy("fetch-user-data"));
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

        final UserData userData = GeotownApplication.getGeotown().userdata().get().execute();

        GeotownApplication.mHandler.post(new Runnable() {
            @Override
            public void run() {
                GeotownApplication.getEventBus().post(new CurrentUserDataReceivedEvent(userData));
            }
        });
    }

    @Override
    protected void onCancel() {

    }

}
