package de.happycarl.geotown.app.api.requests;

import android.os.AsyncTask;

import com.appspot.drive_log.geotown.model.UserData;

import java.io.IOException;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.UsernameSetEvent;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class SetUsernameRequest extends AsyncTask<String, Void, UserData> {
    @Override
    protected UserData doInBackground(String... names) {
        String name = names[0];
        if (name == null || name.isEmpty()) return null;
        UserData data = null;
        try {
            data = GeotownApplication.getGeotown().userdata().setUsername(name).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    @Override
    protected void onPostExecute(UserData data) {
        GeotownApplication.getEventBus().post(new UsernameSetEvent(data));
    }
}
