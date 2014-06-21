package de.happycarl.geotown.app.api.requests;

import android.os.AsyncTask;

import com.appspot.drive_log.geotown.model.Route;

import java.io.IOException;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.RouteDataReceivedEvent;

/**
 * Created by ole on 19.06.14.
 */
public class RouteRequest extends AsyncTask<Long, Void, Route> {

    @Override
    protected Route doInBackground(Long... ids) {
        Route route = null;
        if (ids[0] == null) {
            return null;
        }
        try {
            route = GeotownApplication.getGeotown().routes().get(ids[0]).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return route;
    }

    @Override
    protected void onPostExecute(Route route) {
        GeotownApplication.getEventBus().post(new RouteDataReceivedEvent(route));
    }
}
