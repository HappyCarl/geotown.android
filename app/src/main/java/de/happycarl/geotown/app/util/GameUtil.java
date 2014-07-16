package de.happycarl.geotown.app.util;

import android.content.Context;

import com.google.android.gms.games.Games;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;

/**
 * Created by jhbruhn on 16.07.14.
 */
public class GameUtil {
    public static void publishRouteFinishToPlayGames(Context c) {
        int currCount = GeotownApplication.getPreferences().getInt(AppConstants.PREF_SCORE_ROUTE, 0);
        if(currCount == 0) {
            unlockAchievement(c.getString(R.string.achievment_1route));
        }
        incrementAchievement(c.getString(R.string.achievment_2route), 1);
        incrementAchievement(c.getString(R.string.achievment_5route), 1);
        incrementAchievement(c.getString(R.string.achievment_10route), 1);
        incrementAchievement(c.getString(R.string.achievment_20route), 1);

        incrementEvent(c.getString(R.string.event_routes), 1);
        submitScore(c.getString(R.string.leaderboard_routes), currCount++);
        GeotownApplication.getPreferences().edit().putInt(AppConstants.PREF_SCORE_ROUTE, currCount++).apply();

    }

    public static void publishWaypointFinishToPlayGames(Context c) {
        int currCount = GeotownApplication.getPreferences().getInt(AppConstants.PREF_SCORE_WAYPOINT, 0);

        incrementEvent(c.getString(R.string.event_waypoints), 1);
        submitScore(c.getString(R.string.leaderboard_waypoints), currCount++);

        GeotownApplication.getPreferences().edit().putInt(AppConstants.PREF_SCORE_WAYPOINT, currCount++).apply();
    }

    public static void unlockAchievement(String id) {
        if(GeotownApplication.getGameHelper().isSignedIn())
            Games.Achievements.unlock(GeotownApplication.getGameHelper().getApiClient(), id);
    }

    public static void incrementAchievement(String id, int num) {
        if(GeotownApplication.getGameHelper().isSignedIn())
            Games.Achievements.increment(GeotownApplication.getGameHelper().getApiClient(), id, num);
    }

    public static void incrementEvent(String id, int num) {
        if(GeotownApplication.getGameHelper().isSignedIn())
            Games.Events.increment(GeotownApplication.getGameHelper().getApiClient(), id, num);
    }

    public static void submitScore(String id, int score) {
        if(GeotownApplication.getGameHelper().isSignedIn())
            Games.Leaderboards.submitScore(GeotownApplication.getGameHelper().getApiClient(), id, score);
    }
}
