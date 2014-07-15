package de.happycarl.geotown.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.appspot.drive_log.geotown.Geotown;
import com.google.android.gms.games.Games;
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

    public static void publishRouteFinishToPlayGames(Context c) {
        int currCount = mPreferences.getInt(AppConstants.PREF_SCORE_ROUTE, 0);
        if(currCount == 0) {
            unlockAchievement(c.getString(R.string.achievment_1route));
        }
        incrementAchievement(c.getString(R.string.achievment_2route), 1);
        incrementAchievement(c.getString(R.string.achievment_5route), 1);
        incrementAchievement(c.getString(R.string.achievment_10route), 1);
        incrementAchievement(c.getString(R.string.achievment_20route), 1);

        incrementEvent(c.getString(R.string.event_routes), 1);
        submitScore(c.getString(R.string.leaderboard_routes), currCount++);
        mPreferences.edit().putInt(AppConstants.PREF_SCORE_ROUTE, currCount++).apply();

    }

    public static void publishWaypointFinishToPlayGames(Context c) {
        int currCount = mPreferences.getInt(AppConstants.PREF_SCORE_WAYPOINT, 0);

        incrementEvent(c.getString(R.string.event_waypoints), 1);
        submitScore(c.getString(R.string.leaderboard_waypoints), currCount++);

        mPreferences.edit().putInt(AppConstants.PREF_SCORE_WAYPOINT, currCount++).apply();
    }

    public static void unlockAchievement(String id) {
        if(mGameHelper.isSignedIn())
            Games.Achievements.unlock(mGameHelper.getApiClient(), id);
    }

    public static void incrementAchievement(String id, int num) {
        if(mGameHelper.isSignedIn())
            Games.Achievements.increment(mGameHelper.getApiClient(), id, num);
    }

    public static void incrementEvent(String id, int num) {
        if(mGameHelper.isSignedIn())
            Games.Events.increment(mGameHelper.getApiClient(), id, num);
    }

    public static void submitScore(String id, int score) {
        if(mGameHelper.isSignedIn())
            Games.Leaderboards.submitScore(mGameHelper.getApiClient(), id, score);
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
