package de.happycarl.geotown.app.events.net;

import com.appspot.drive_log.geotown.model.Route;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class MyRoutesDataReceivedEvent {
    public ImmutableList<Route> routes;

    public MyRoutesDataReceivedEvent(List<Route> routes) {
        if (routes != null)
            this.routes = ImmutableList.copyOf(routes);
    }
}
