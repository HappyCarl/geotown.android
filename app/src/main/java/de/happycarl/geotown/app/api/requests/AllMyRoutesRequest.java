package de.happycarl.geotown.app.api.requests;

import android.os.AsyncTask;
import android.util.Log;

import com.appspot.drive_log.geotown.model.Route;
import com.appspot.drive_log.geotown.model.RouteCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.MyRoutesDataReceivedEvent;

/**
 * Created by ole on 18.06.14.
 */
public class AllMyRoutesRequest extends AsyncTask<Void, Void, RouteCollection> {

    @Override
    protected RouteCollection doInBackground(Void... params) {
        RouteCollection rc = null;

        try {
            rc = GeotownApplication.getGeotown().routes().listMine().execute();

        } catch (IOException e) {

            Log.d("AllMyRoutesRequest", "ERROR:" + e.toString());

        }

        return rc;
    }

    @Override
    protected void onPostExecute(RouteCollection routeCollection) {
        List<Route> routes = new ArrayList<>();
        if (routeCollection != null && routeCollection.getItems() != null) {
            routes = routeCollection.getItems();
        }
        GeotownApplication.getEventBus().post(new MyRoutesDataReceivedEvent(routes));

    }
}
