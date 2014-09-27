package de.happycarl.geotown.app.util;

import android.content.Context;

import com.google.android.gms.games.Games;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.GameHelper;

/**
 * Created by jhbruhn on 16.07.14.
 */
public class GameUtil {
    public static void publishRouteFinishToPlayGames(Context c, GameHelper gameHelper) {
        int currCount = GeotownApplication.getPreferences().getInt(AppConstants.PREF_SCORE_ROUTE, 0);
        if(currCount == 0) {
            unlockAchievement(c.getString(R.string.achievment_1route), gameHelper);
        }
        incrementAchievement(c.getString(R.string.achievment_2route), 1, gameHelper);
        incrementAchievement(c.getString(R.string.achievment_5route), 1, gameHelper);
        incrementAchievement(c.getString(R.string.achievment_10route), 1, gameHelper);
        incrementAchievement(c.getString(R.string.achievment_20route), 1, gameHelper);

        incrementEvent(c.getString(R.string.event_routes), 1, gameHelper);
        submitScore(c.getString(R.string.leaderboard_routes), currCount++, gameHelper);
        GeotownApplication.getPreferences().edit().putInt(AppConstants.PREF_SCORE_ROUTE, currCount++).apply();

    }

    public static void publishWaypointFinishToPlayGames(Context c, GameHelper gameHelper) {
        int currCount = GeotownApplication.getPreferences().getInt(AppConstants.PREF_SCORE_WAYPOINT, 0);

        incrementEvent(c.getString(R.string.event_waypoints), 1, gameHelper);
        submitScore(c.getString(R.string.leaderboard_waypoints), currCount++, gameHelper);

        GeotownApplication.getPreferences().edit().putInt(AppConstants.PREF_SCORE_WAYPOINT, currCount++).apply();
    }

    public static void unlockAchievement(String id, GameHelper gameHelper) {
        if(gameHelper.isSignedIn())
            Games.Achievements.unlock(gameHelper.getApiClient(), id);
    }

    public static void incrementAchievement(String id, int num, GameHelper gameHelper) {
        if(gameHelper.isSignedIn())
            Games.Achievements.increment(gameHelper.getApiClient(), id, num);
    }

    public static void incrementEvent(String id, int num, GameHelper gameHelper) {
        if(gameHelper.isSignedIn())
            Games.Events.increment(gameHelper.getApiClient(), id, num);
    }

    public static void submitScore(String id, int score, GameHelper gameHelper) {
        if(gameHelper.isSignedIn())
            Games.Leaderboards.submitScore(gameHelper.getApiClient(), id, score);
    }
}
