package de.happycarl.geotown.app;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
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
import de.happycarl.geotown.app.api.GameHelper;
import de.happycarl.geotown.app.events.ApplicationStartedEvent;
import de.happycarl.geotown.app.events.google.GoogleClientConnectedEvent;
import de.happycarl.geotown.app.events.google.GoogleClientConnectionFailedEvent;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class GeotownApplication extends Application implements GameHelper.GameHelperListener {

    public static Handler mHandler = new Handler();

    private static Bus mEventBus;
    private static SharedPreferences mPreferences;
    private static Geotown mGeotown;
    private static JobManager mJobManager;
    private static GameHelper mGameHelper;


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

    public static GameHelper getGameHelper() {
        return mGameHelper;
    }

    public void login(GoogleAccountCredential cred) {
        mGeotown = ApiUtils.getApiServiceHandle(cred);
    }

    public void googleLogin(Activity a) {
        Log.i("PEDAB", "Logging in...");
        mGameHelper = new GameHelper(a, GameHelper.CLIENT_GAMES);
        mGameHelper.setConnectOnStart(true);
        mGameHelper.enableDebugLog(true);
        mGameHelper.setup(this);
        mGameHelper.onStart(a);
    }

    public static long intsToLong(int part1, int part2) {
        return (long) part1 << 32 | part2 & 0xFFFFFFFFL;
    }

    public static int[] longToInts(long num) {
        int[] res = new int[2];
        res[0] = (int) (num >> 32);
        res[1] = (int) num;
        return res;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ActiveAndroid.initialize(this);

        mEventBus = new Bus(ThreadEnforcer.MAIN);
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

    @Override
    public void onSignInFailed() {
        Log.i("PEDAB", "Sign In Failed");
        mEventBus.post(new GoogleClientConnectionFailedEvent());
    }

    @Override
    public void onSignInSucceeded() {
        Log.i("PEDAB", "Sign In Succeeded");
        mEventBus.post(new GoogleClientConnectedEvent());
    }
}
