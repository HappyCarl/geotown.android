package de.happycarl.geotown.app.api.requests;

import com.appspot.drive_log.geotown.model.UserData;
import com.path.android.jobqueue.Params;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.UsernameSetEvent;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class SetUsernameRequest extends NetworkRequestJob {
    private final String name;

    public SetUsernameRequest(String name) {
        super(new Params(3).requireNetwork().persist().groupBy("set-username"));
        this.name = name;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        final UserData data = GeotownApplication.getGeotown().userdata().setUsername(name).execute();
        GeotownApplication.mHandler.post(new Runnable() {
            @Override
            public void run() {
                GeotownApplication.getEventBus().post(new UsernameSetEvent(data));
            }
        });
    }

    @Override
    protected void onCancel() {

    }

}
