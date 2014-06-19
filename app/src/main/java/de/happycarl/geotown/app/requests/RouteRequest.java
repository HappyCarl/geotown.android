package de.happycarl.geotown.app.requests;

import android.os.AsyncTask;

import com.appspot.drive_log.geotown.model.Route;

import java.io.IOException;

import de.happycarl.geotown.app.AppConstants;

/**
 * Created by ole on 19.06.14.
 */
public class RouteRequest extends AsyncTask<Long, Void,Route> {

    RequestDataReceiver requestDataReceiver;

    public RouteRequest(RequestDataReceiver receiver) {
        this.requestDataReceiver = receiver;
    }

    @Override
    protected Route doInBackground(Long... ids) {
        Route route = null;
        if (ids[0] == null) {
            return null;
        }
        try {
            route = AppConstants.geoTownInstance.geoTownEndpoints().getRoute(ids[0]).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return route;
    }

    @Override
    protected void onPostExecute(Route route) {
        requestDataReceiver.onRequestedData(AppConstants.REQUEST_ROUTE, route);
    }
}
