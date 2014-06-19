package de.happycarl.geotown.app.api.requests;

import android.os.AsyncTask;

import com.appspot.drive_log.geotown.model.UserData;

import java.io.IOException;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.api.ApiUtils;
import de.happycarl.geotown.app.events.CurrentUserDataReceivedEvent;

/**
 * Created by ole on 19.06.14.
 */
public class CurrentUserDataRequest extends AsyncTask<Void, Void, UserData> {

    @Override
    protected UserData doInBackground(Void... params) {
        UserData userData = null;

        try {
            userData = GeotownApplication.getGeotown().userdata().get().execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return userData;
    }

    @Override
    protected void onPostExecute(UserData userData) {
        GeotownApplication.getEventBus().post(new CurrentUserDataReceivedEvent(userData));
    }
}
