package de.happycarl.geotown.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.appspot.drive_log.geotown.Geotown;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import com.path.android.jobqueue.log.CustomLogger;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import de.happycarl.geotown.app.api.ApiUtils;
import de.happycarl.geotown.app.events.ApplicationStartedEvent;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class GeotownApplication extends Application {
    private static Bus mEventBus;
    private static SharedPreferences mPreferences;
    private static Geotown mGeotown;
    private static JobManager mJobManager;

    public static Bus getEventBus() {
        return mEventBus;
    }

    public static SharedPreferences getPreferences() {
        return mPreferences;
    }

    public static Geotown getGeotown() {
        return mGeotown;
    }

    public static JobManager getJobManager() {
        return mJobManager;
    }

    public static void login(GoogleAccountCredential cred) {
        mGeotown = ApiUtils.getApiServiceHandle(cred);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ActiveAndroid.initialize(this);

        mEventBus = new Bus(ThreadEnforcer.ANY);
        mPreferences = getSharedPreferences(AppConstants.PREF_NAME, 0);
        mGeotown = ApiUtils.getApiServiceHandle(null);

        configureJobManager();

        mEventBus.post(new ApplicationStartedEvent(this));

    }

    private void configureJobManager() {
        Configuration configuration = new Configuration.Builder(this)
                .customLogger(new CustomLogger() {
                    private static final String TAG = "JOBS";

                    @Override
                    public boolean isDebugEnabled() {
                        return false;
                    }

                    @Override
                    public void d(String text, Object... args) {
                        Log.d(TAG, String.format(text, args));
                    }

                    @Override
                    public void e(Throwable t, String text, Object... args) {
                        Log.e(TAG, String.format(text, args), t);
                    }

                    @Override
                    public void e(String text, Object... args) {
                        Log.e(TAG, String.format(text, args));
                    }
                })
                .minConsumerCount(1)//always keep at least one consumer alive
                .maxConsumerCount(3)//up to 3 consumers at a time
                .loadFactor(3)//3 jobs per consumer
                .consumerKeepAlive(120)//wait 2 minute
                .build();
        mJobManager = new JobManager(this, configuration);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ActiveAndroid.dispose();
    }
}
