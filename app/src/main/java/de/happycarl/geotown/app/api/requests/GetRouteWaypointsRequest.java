package de.happycarl.geotown.app.api.requests;

import android.os.AsyncTask;
import android.util.Log;

import com.appspot.drive_log.geotown.Geotown;
import com.appspot.drive_log.geotown.model.Route;
import com.appspot.drive_log.geotown.model.RouteCollection;
import com.appspot.drive_log.geotown.model.Waypoint;
import com.appspot.drive_log.geotown.model.WaypointCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.MyRoutesDataReceivedEvent;
import de.happycarl.geotown.app.events.RouteWaypointsReceivedEvent;

/**
 * Created by ole on 20.06.14.
 */
public class GetRouteWaypointsRequest extends AsyncTask<Long, Void, WaypointCollection> {

    @Override
    protected WaypointCollection doInBackground(Long... params) {
        WaypointCollection wc = null;

        Log.d("WaypointRequest","Getting waypoints for route" + params[0]);
        try {
            wc = GeotownApplication.getGeotown().waypoints().list(params[0]).execute();

        } catch (IOException e) {

            Log.d("GetRouteWaypointsRequest", "ERROR:" + e.toString());

        }

        return wc;
    }

    @Override
    protected void onPostExecute(WaypointCollection list) {
        List<Waypoint> waypoints = new ArrayList<>();
        if(list.getItems() != null) {
            waypoints = list.getItems();
        }

        GeotownApplication.getEventBus().post(new RouteWaypointsReceivedEvent(waypoints));

    }
}
