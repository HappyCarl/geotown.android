package de.happycarl.geotown.app.events.net;

/**
 * Created by ole on 28.09.14.
 */
public class TrackStartedEvent {
    public final long trackId;

    public TrackStartedEvent(long id) { trackId = id;}
}
