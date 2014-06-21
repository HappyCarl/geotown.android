package de.happycarl.geotown.app.models;

import android.os.AsyncTask;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.appspot.drive_log.geotown.model.Route;

import java.util.List;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.GeoTownRouteRetrievedEvent;
import de.happycarl.geotown.app.events.GeoTownRouteSavedEvent;

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

    public List<GeoTownWaypoint> waypoints() {
        return getMany(GeoTownWaypoint.class, "route");
    }

    @Column(name = "mine")
    public boolean mine;

    public static void getRoute(long id, int reqId) {
        new GetRouteByIdAsyncTask(reqId).execute(id);
    }

    public static void getRoute(String name, int reqId) {
        new GetRouteByNameAsyncTask(reqId).execute(name);
    }

    public static void update(Route r, boolean createIfNotExist) {
        UpdateRouteAsyncTask.UpdateRouteParams p = new UpdateRouteAsyncTask.UpdateRouteParams();
        p.route = r;
        p.createIfNotExist = createIfNotExist;
        new UpdateRouteAsyncTask().execute(p);
    }

    private static class UpdateRouteAsyncTask extends AsyncTask<UpdateRouteAsyncTask.UpdateRouteParams, Void, GeoTownRoute> {
        public static class UpdateRouteParams {
            Route route;
            boolean createIfNotExist;
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
            route.name = r.getName();
            route.latitude = r.getLatitude();
            route.longitude = r.getLongitude();
            route.owner = r.getOwner().getUsername();
            route.mine = true;
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
