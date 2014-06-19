package de.happycarl.geotown.app.events;

import com.appspot.drive_log.geotown.model.UserData;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class CurrentUserDataReceivedEvent {
    public final UserData userData;

    public CurrentUserDataReceivedEvent(UserData userData) {
        this.userData = userData;
    }
}
