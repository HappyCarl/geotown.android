package de.happycarl.geotown.app.models;

import android.os.AsyncTask;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.appspot.drive_log.geotown.model.Route;
import com.appspot.drive_log.geotown.model.Waypoint;

import java.util.List;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.db.GeoTownForeignRoutesRetrievedEvent;
import de.happycarl.geotown.app.events.db.GeoTownRouteDeletedEvent;
import de.happycarl.geotown.app.events.db.GeoTownRouteRetrievedEvent;
import de.happycarl.geotown.app.events.db.GeoTownRouteSavedEvent;

/**
 * Created by ole on 20.06.14.
 */

@Table(name = "Routes")
public class GeoTownRoute extends Model {

    @Column(name = "name", index = true)
    public String name;

    @Column(name = "owner")
    public String owner;

    @Column(name = "routeID", index = true, unique = true)
    public long id;

    @Column(name = "lat")
    public double latitude;

    @Column(name = "long")
    public double longitude;

    @Column(name = "mine")
    public boolean mine;

    @Column(name = "nearIndex")
    public int nearIndex;

    @Column(name = "starred")
    public boolean starred;

    public List<GeoTownWaypoint> waypoints() {
        return getMany(GeoTownWaypoint.class, "route");
    }

    public static void getRoute(long id, int reqId) {
        new GetRouteByIdAsyncTask(reqId).execute(id);
    }


    // TODO: Clean this mess up.
    public static void update(Route r, int nearIndex, boolean createIfNotExist, boolean sync) {
        UpdateRouteAsyncTask.UpdateRouteParams p = new UpdateRouteAsyncTask.UpdateRouteParams();
        p.route = r;
        p.nearIndex = nearIndex;
        p.createIfNotExist = createIfNotExist;
        if(sync)
            new UpdateRouteAsyncTask().doInBackground(p);
        else
            new UpdateRouteAsyncTask().execute(p);
    }

    public static void starRoute(Route r, boolean star) {
        UpdateRouteAsyncTask.UpdateRouteParams p = new UpdateRouteAsyncTask.UpdateRouteParams();
        p.route = r;
        p.star = star;
        p.updateStar = true;
        new UpdateRouteAsyncTask().execute(p);
    }

    public static void update(Route r, boolean createIfNotExist) {
        update(r, -1, createIfNotExist, false);
    }


    private static class UpdateRouteAsyncTask extends AsyncTask<UpdateRouteAsyncTask.UpdateRouteParams, Void, GeoTownRoute> {
        public static class UpdateRouteParams {
            Route route;
            boolean createIfNotExist;
            int nearIndex;
            boolean star;
            boolean updateStar;
        }

        @Override
        protected GeoTownRoute doInBackground(UpdateRouteParams... params) {
            UpdateRouteParams p = params[0];
            Route r = p.route;
            GeoTownRoute route = new Select()
                    .from(GeoTownRoute.class)
                    .where("name = ?", p.route.getName())
                    .limit(1)
                    .executeSingle();
            if (route == null) {
                if (!p.createIfNotExist)
                    return null;
                route = new GeoTownRoute();
            }
            route.id = r.getId();
            route.nearIndex = p.nearIndex;
            route.name = r.getName();
            route.latitude = r.getLatitude();
            route.longitude = r.getLongitude();
            route.owner = r.getOwner().getUsername();
            if(p.updateStar)
                route.starred = p.star;
            String user = GeotownApplication.getPreferences().getString(AppConstants.PREF_ACCOUNT_EMAIL, "");
            route.mine = r.getOwner().getEmail().equals(user);
            route.save();
            return route;
        }

        @Override
        protected void onPostExecute(GeoTownRoute route) {
            GeotownApplication.getEventBus().post(new GeoTownRouteSavedEvent(route));
        }
    }

    private static class GetRouteByNameAsyncTask extends AsyncTask<String, Void, GeoTownRoute> {
        int id;

        public GetRouteByNameAsyncTask(int id) {
            this.id = id;
        }

        @Override
        protected GeoTownRoute doInBackground(String... params) {
            String n = params[0];
            GeoTownRoute route = new Select()
                    .from(GeoTownRoute.class)
                    .where("name = ?", n)
                    .limit(1)
                    .executeSingle();
            return route;
        }

        @Override
        protected void onPostExecute(GeoTownRoute route) {
            GeotownApplication.getEventBus().post(new GeoTownRouteRetrievedEvent(route, id));
        }
    }

    private static class GetRouteByIdAsyncTask extends AsyncTask<Long, Void, GeoTownRoute> {
        private int id;

        public GetRouteByIdAsyncTask(int id) {
            this.id = id;
        }

        @Override
        protected GeoTownRoute doInBackground(Long... params) {
            Long n = params[0];
            GeoTownRoute route = new Select()
                    .from(GeoTownRoute.class)
                    .where("routeID = ?", n)
                    .limit(1)
                    .executeSingle();
            return route;
        }

        @Override
        protected void onPostExecute(GeoTownRoute route) {
            GeotownApplication.getEventBus().post(new GeoTownRouteRetrievedEvent(route, this.id));
        }
    }
}
