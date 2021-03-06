package de.happycarl.geotown.app.api.requests;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;
import com.appspot.drive_log.geotown.model.Route;
import com.appspot.drive_log.geotown.model.Waypoint;
import com.path.android.jobqueue.Params;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.RouteDataReceivedEvent;
import de.happycarl.geotown.app.models.GeoTownRoute;
import de.happycarl.geotown.app.models.GeoTownWaypoint;
import de.happycarl.geotown.app.util.GeocoderUtil;

/**
 * Created by ole on 19.06.14.
 */
public class RouteRequest extends NetworkRequestJob {
    private final long routeId;

    public RouteRequest(long id) {
        super(new Params(3).requireNetwork().groupBy("fetch-route"));

        this.routeId = id;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        Route r = GeotownApplication.getGeotown().routes().get(routeId).execute();

        List<Waypoint> waypoints = GeotownApplication.getGeotown().waypoints().list(routeId).execute().getItems();

        GeoTownRoute route = new Select().from(GeoTownRoute.class).where("routeID = ?", r.getId()).executeSingle();
        if (route == null)
            route = new GeoTownRoute();
        route.location = GeocoderUtil.geocodeLocation(r.getLatitude(), r.getLongitude(), GeotownApplication.getContext());
        route.id = r.getId();
        route.name = r.getName();
        route.latitude = r.getLatitude();
        route.longitude = r.getLongitude();
        route.owner = r.getOwner().getUsername();
        String user = GeotownApplication.getPreferences().getString(AppConstants.PREF_ACCOUNT_EMAIL, "");
        route.mine = r.getOwner().getEmail().equals(user);

        ActiveAndroid.beginTransaction();
        try {
            if (waypoints != null)
                for (Waypoint w : waypoints) {
                    GeoTownWaypoint wp = new GeoTownWaypoint(w, route);
                    GeoTownWaypoint oldwp = new Select().from(GeoTownWaypoint.class).where("WaypointID = ?", w.getId()).executeSingle();
                    if (oldwp == null) {
                        wp.save();
                    }
                    Picasso.with(GeotownApplication.getContext()).load(w.getImageUrl()).fetch();
                }
            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }

        route.save();

        final Route pedaB = r;
        GeotownApplication.mHandler.post(new Runnable() {
            @Override
            public void run() {
                GeotownApplication.getEventBus().post(new RouteDataReceivedEvent(pedaB));
            }
        });

    }

    @Override
    protected void onCancel() {

    }

}
