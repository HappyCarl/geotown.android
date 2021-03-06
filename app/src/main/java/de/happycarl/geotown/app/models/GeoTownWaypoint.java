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

    @Column(name = "wrongAnswers")
    public String wrongAnswers; //Seperated by >|<

    @Column(name = "rightAnswer")
    public String rightAnswer;

    @Column(name = "route")
    public GeoTownRoute route;

    @Column(name = "done")
    public boolean done;

    @Column(name = "imageURL")
    public String imageURL;

    public GeoTownWaypoint(Waypoint w, GeoTownRoute r) {
        this.id = w.getId();
        this.question = w.getQuestion();
        String ans = "";
        if (w.getWrongAnswers() != null)
            for (String s : w.getWrongAnswers()) {
                ans += s + "|";
            }
        this.wrongAnswers = ans;
        this.rightAnswer = w.getRightAnswer();
        this.latitude = w.getLatitude();
        this.longitude = w.getLongitude();
        this.imageURL = w.getImageUrl();
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

        return id == that.id;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (id ^ (id >>> 32));
        return result;
    }
}
