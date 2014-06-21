package de.happycarl.geotown.app.events;

import de.happycarl.geotown.app.models.GeoTownRoute;

/**
 * Created by jhbruhn on 21.06.14.
 */
public class GeoTownRouteRetrievedEvent {
    public GeoTownRoute route;
    public int id;

    public GeoTownRouteRetrievedEvent(GeoTownRoute route, int id) {
        this.route = route;
        this.id = id;
    }
}
