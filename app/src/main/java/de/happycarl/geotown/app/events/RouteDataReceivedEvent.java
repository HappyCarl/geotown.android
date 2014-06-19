package de.happycarl.geotown.app.events;

import com.appspot.drive_log.geotown.model.Route;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class RouteDataReceivedEvent {
    public final Route route;

    public RouteDataReceivedEvent(Route route) {
        this.route = route;
    }
}
