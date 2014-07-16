package de.happycarl.geotown.app.events.db;

/**
 * Created by ole on 22.06.14.
 */
public class GeoTownRouteDeletedEvent {
    final long routeId;
    public GeoTownRouteDeletedEvent(long id) {
        routeId = id;
    }
}
