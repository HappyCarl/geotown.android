package de.happycarl.geotown.app.events.db;

/**
 * Created by ole on 22.06.14.
 */
public class GeoTownWaypointsAddedEvent {
    public final boolean success;

    public GeoTownWaypointsAddedEvent(boolean success) {
        this.success = success;
    }
}
