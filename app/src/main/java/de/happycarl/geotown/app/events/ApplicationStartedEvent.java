package de.happycarl.geotown.app.events;

import android.app.Application;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class ApplicationStartedEvent {
    public Application application;

    public ApplicationStartedEvent(Application application) {
        this.application = application;
    }
}
