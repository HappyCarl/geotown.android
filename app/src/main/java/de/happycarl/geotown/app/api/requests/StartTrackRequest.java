package de.happycarl.geotown.app.api.requests;

import android.util.Log;

import com.appspot.drive_log.geotown.Geotown;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.path.android.jobqueue.Params;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.ApiUtils;
import de.happycarl.geotown.app.events.net.TrackStartedEvent;

/**
 * Created by ole on 28.09.14.
 */
public class StartTrackRequest extends NetworkRequestJob {

    long routeId;

    public StartTrackRequest(long routeId) {
        super(new Params(1).requireNetwork().persist().groupBy("start-track"));
        this.routeId = routeId;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        GoogleAccountCredential cred = GoogleAccountCredential.usingAudience(GeotownApplication.getContext(), GeotownApplication.getContext().getResources().getString(R.string.client_id));
        cred.setSelectedAccountName(GeotownApplication.getPreferences().getString(AppConstants.PREF_ACCOUNT_EMAIL, ""));
        Geotown gt = ApiUtils.getApiServiceHandle(cred);

        final long trackId = gt.tracks().startTrack(routeId).execute().getId();
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
