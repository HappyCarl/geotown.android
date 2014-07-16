package de.happycarl.geotown.app.events.db;

import java.util.List;

import de.happycarl.geotown.app.models.GeoTownRoute;

/**
 * Created by ole on 22.06.14.
 */
public class GeoTownForeignRoutesRetrievedEvent {
    public final List<GeoTownRoute> routes;
    public final int reqId;

    public GeoTownForeignRoutesRetrievedEvent(List<GeoTownRoute> routes, int id) {
        this.reqId = id;
        this.routes = routes;
    }

}
