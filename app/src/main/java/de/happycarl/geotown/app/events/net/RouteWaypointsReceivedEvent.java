package de.happycarl.geotown.app.events.net;

import com.appspot.drive_log.geotown.model.Waypoint;

import java.util.List;

/**
 * Created by ole on 20.06.14.
 */
public class RouteWaypointsReceivedEvent {
    public final List<Waypoint> waypoints;

    public RouteWaypointsReceivedEvent(List<Waypoint> wps) {
        waypoints = wps;
    }
}
