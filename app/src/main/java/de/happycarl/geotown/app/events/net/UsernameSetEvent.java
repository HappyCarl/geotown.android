package de.happycarl.geotown.app.events.net;

import com.appspot.drive_log.geotown.model.UserData;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class UsernameSetEvent {
    public final UserData userData;

    public UsernameSetEvent(UserData userData) {
        this.userData = userData;
    }
}
