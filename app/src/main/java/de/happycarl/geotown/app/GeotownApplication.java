package de.happycarl.geotown.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.appspot.drive_log.geotown.Geotown;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import com.path.android.jobqueue.log.CustomLogger;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import de.happycarl.geotown.app.api.ApiUtils;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class GeotownApplication extends Application {

    public static final Handler mHandler = new Handler();

    private static Bus mEventBus;
    private static SharedPreferences mPreferences;
    private static Geotown mGeotown;
    private static JobManager mJobManager;
    private static Context mContext;

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

    public static Context getContext() {return mContext;}


    private Tracker mTracker;


    public void doServerLogin(GoogleAccountCredential cred) {
        mGeotown = ApiUtils.getApiServiceHandle(cred);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ActiveAndroid.initialize(this);

        mEventBus = new Bus(ThreadEnforcer.MAIN);
        mPreferences = getSharedPreferences(AppConstants.PREF_NAME, MODE_MULTI_PROCESS);
        mGeotown = ApiUtils.getApiServiceHandle(null);

        configureJobManager();

        mContext = this.getApplicationContext();

        mTracker = GoogleAnalytics.getInstance(this).newTracker(R.xml.tracker);
        mTracker.enableAdvertisingIdCollection(true);

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

    public Tracker getTracker() {
        return mTracker;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ActiveAndroid.dispose();
    }
}
