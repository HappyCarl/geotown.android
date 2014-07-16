package de.happycarl.geotown.app.events.db;

import de.happycarl.geotown.app.models.GeoTownRoute;

/**
 * Created by jhbruhn on 21.06.14.
 */
public class GeoTownRouteRetrievedEvent {
    public final GeoTownRoute route;
    public final int id;

    public GeoTownRouteRetrievedEvent(GeoTownRoute route, int id) {
        this.route = route;
        this.id = id;
    }
}
