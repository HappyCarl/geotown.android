package de.happycarl.geotown.app.api.requests;

import android.os.AsyncTask;
import android.util.Log;

import com.appspot.drive_log.geotown.model.Route;
import com.appspot.drive_log.geotown.model.RouteCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.NearRoutesDataReceivedEvent;

/**
 * Created by jhbruhn on 20.06.14.
 */
public class NearRoutesRequest extends AsyncTask<NearRoutesRequest.NearRoutesParams, Void, RouteCollection> {
    public static class NearRoutesParams {
        public double lat, lng, radius;

        public NearRoutesParams(double lat, double lng, double radius) {
            this.lat = lat;
            this.lng = lng;
            this.radius = radius;
        }
    }

    @Override
    protected RouteCollection doInBackground(NearRoutesParams... params) {
        RouteCollection rc = null;
        NearRoutesParams p = params[0];
        try {
            rc = GeotownApplication.getGeotown().routes().listNear(p.lat, p.lng, p.radius).execute();

        } catch (IOException e) {

            Log.d("NearRoutesRequest", "ERROR:" + e.toString());

        }

        return rc;
    }

    @Override
    protected void onPostExecute(RouteCollection routeCollection) {
        List<Route> routes = new ArrayList<>();
        if (routeCollection != null && routeCollection.getItems() != null) {
            routes = routeCollection.getItems();
        }
        GeotownApplication.getEventBus().post(new NearRoutesDataReceivedEvent(routes));

    }
}
