package de.happycarl.geotown.app.models;

import android.os.AsyncTask;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.appspot.drive_log.geotown.model.Waypoint;

import java.util.List;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.db.GeoTownWaypointsAddedEvent;

/**
 * Created by ole on 20.06.14.
 */

@Table(name = "Waypoints")
public class GeoTownWaypoint extends Model {


    @Column(name = "WaypointID", index = true)
    public long id;

    @Column(name = "lat")
    public double latitude;

    @Column(name = "long")
    public double longitude;

    @Column(name = "question")
    public String question;

    @Column(name = "answers")
    public String answers; //Seperated by >|<

    @Column(name = "route")
    public GeoTownRoute route;

    @Column(name= "done")
    public boolean done;

    public static void addWaypoints(List<Waypoint> wp) {
        new AddWaypointsAsyncTask().execute(wp);
    }

    private static class AddWaypointsAsyncTask extends AsyncTask<List<Waypoint>,Void,Boolean> {

        @Override
        protected Boolean doInBackground(List<Waypoint>... params) {
            ActiveAndroid.beginTransaction();
            try {
                for (Waypoint w : params[0]) {
                    GeoTownWaypoint geoTownWaypoint = new GeoTownWaypoint();
                    geoTownWaypoint.id = w.getId();
                    geoTownWaypoint.question = w.getQuestion();
                    String ans = "";
                    for (String s : w.getAnswers()) {
                        ans += s + ">|<";
                    }
                    geoTownWaypoint.answers = ans;
                    geoTownWaypoint.latitude = w.getLatitude();
                    geoTownWaypoint.longitude = w.getLongitude();
                    geoTownWaypoint.route = new Select().from(GeoTownRoute.class).where("routeID = ?",w.getRoute().getId()).executeSingle();
                    geoTownWaypoint.save();
                    Log.d("Waypoints",w.getQuestion() + w.getId());
                }
                ActiveAndroid.setTransactionSuccessful();
            } finally {
                ActiveAndroid.endTransaction();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean param) {
            GeotownApplication.getEventBus().post(new GeoTownWaypointsAddedEvent(param));
        }
    }

}
