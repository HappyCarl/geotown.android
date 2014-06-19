package de.happycarl.geotown.app;

import android.app.Application;
import android.content.SharedPreferences;

import com.appspot.drive_log.geotown.Geotown;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import java.util.prefs.Preferences;

import de.happycarl.geotown.app.api.ApiUtils;
import de.happycarl.geotown.app.events.ApplicationStartedEvent;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class GeotownApplication extends Application {
    private static Bus mEventBus;

    public static Bus getEventBus() {
        return mEventBus;
    }

    private static SharedPreferences mPreferences;

    public static SharedPreferences getPreferences() {
        return mPreferences;
    }

    private static Geotown mGeotown;

    public static Geotown getGeotown() {
        return mGeotown;
    }

    public static void login(GoogleAccountCredential cred) {
        mGeotown = ApiUtils.getApiServiceHandle(cred);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mEventBus = new Bus(ThreadEnforcer.ANY);
        mPreferences = getSharedPreferences(AppConstants.PREF_NAME, 0);
        mGeotown = ApiUtils.getApiServiceHandle(null);

        mEventBus.post(new ApplicationStartedEvent(this));
    }
}
