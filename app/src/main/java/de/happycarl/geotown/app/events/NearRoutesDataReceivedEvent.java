package de.happycarl.geotown.app.events;

import com.appspot.drive_log.geotown.model.Route;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by jhbruhn on 20.06.14.
 */
public class NearRoutesDataReceivedEvent {
    public ImmutableList<Route> routes;

    public NearRoutesDataReceivedEvent(List<Route> routes) {
        if (routes != null)
            this.routes = ImmutableList.copyOf(routes);
    }
}
