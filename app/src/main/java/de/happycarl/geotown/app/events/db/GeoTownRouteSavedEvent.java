package de.happycarl.geotown.app.events.db;

import de.happycarl.geotown.app.models.GeoTownRoute;

/**
 * Created by jhbruhn on 21.06.14.
 */
public class GeoTownRouteSavedEvent {
    public final GeoTownRoute route;

    public GeoTownRouteSavedEvent(GeoTownRoute r) {
        this.route = r;
    }
}
