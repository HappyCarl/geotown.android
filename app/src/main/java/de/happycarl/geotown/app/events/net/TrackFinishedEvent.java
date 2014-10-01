package de.happycarl.geotown.app.events.net;

/**
 * Created by ole on 28.09.14.
 */
public class TrackFinishedEvent {
    public final boolean success;

    public TrackFinishedEvent(boolean success) {this.success = success;}
}
