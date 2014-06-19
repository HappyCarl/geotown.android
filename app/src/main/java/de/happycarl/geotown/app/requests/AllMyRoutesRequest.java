package de.happycarl.geotown.app.requests;

import android.os.AsyncTask;
import android.util.Log;

import com.appspot.drive_log.geotown.model.RouteCollection;

import java.io.IOException;

import de.happycarl.geotown.app.AppConstants;

/**
 * Created by ole on 18.06.14.
 */
public class AllMyRoutesRequest extends AsyncTask<Void, Void, RouteCollection>{

    RequestDataReceiver receiver;
    int id;

    public AllMyRoutesRequest(RequestDataReceiver requestDataReceiver) {
        this.receiver = requestDataReceiver;
        this.id = AppConstants.REQUEST_ALL_ROUTES;
    }
    @Override
    protected RouteCollection doInBackground(Void... params) {
        RouteCollection rc = null;

        try {
            rc = AppConstants.geoTownInstance.geoTownEndpoints().getMyRoutes().execute();
        } catch (IOException e) {

            Log.d("AllMyRoutesRequest", "ERROR:" + e.toString());

        }

        return rc;
    }

    @Override
    protected void onPostExecute(RouteCollection routeCollection) {
        receiver.onRequestedData(id,routeCollection);

    }
}
