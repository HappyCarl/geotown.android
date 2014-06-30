package de.happycarl.geotown.app.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.appspot.drive_log.geotown.model.Waypoint;

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

    @Column(name = "done")
    public boolean done;

    public GeoTownWaypoint(Waypoint w, GeoTownRoute r) {
        this.id = w.getId();
        this.question = w.getQuestion();
        String ans = "";
        if (w != null && w.getAnswers() != null)
            for (String s : w.getAnswers()) {
                ans += s + ">|<";
            }
        this.answers = ans;
        this.latitude = w.getLatitude();
        this.longitude = w.getLongitude();
        this.route = r;
    }

    public GeoTownWaypoint() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GeoTownWaypoint that = (GeoTownWaypoint) o;

        if (id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (id ^ (id >>> 32));
        return result;
    }
}
