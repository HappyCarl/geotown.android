package de.happycarl.geotown.app.requests;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.appspot.drive_log.geotown.model.Route;
import com.appspot.drive_log.geotown.model.RouteCollection;

import java.io.IOException;

import de.happycarl.geotown.app.AppConstants;

/**
 * Created by ole on 18.06.14.
 */
public class AllMyRoutesRequest extends AsyncTask<Void, Void, RouteCollection>{

    Context context;

    public AllMyRoutesRequest(Context context) {
        this.context = context;
    }
    @Override
    protected RouteCollection doInBackground(Void... params) {
        RouteCollection rc = null;

        try {
            rc = AppConstants.geotownInstance.geoTownEndpoints().getMyRoutes().execute();
        } catch (IOException e) {

            Log.d("AllMyRoutesRequest", "ERROR:" + e.toString());

        }

        return rc;
    }

    @Override
    protected void onPostExecute(RouteCollection routeCollection) {
        if (routeCollection != null) {
            for(Route r : routeCollection.getItems()) {
                Log.d("AllMyRoutesRequest",r.getName() + " : " + r.getLatitude() + "/" + r.getLongitude());
            }
        } else {
            Log.i("AllMyRoutesRequest","No data returned");
        }
    }
}
