package de.happycarl.geotown.app.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.List;

/**
 * Created by ole on 20.06.14.
 */

@Table(name = "Waypoints")
public class GeoTownWaypoint extends Model{


    @Column(name = "name", index = true)
    public String name;

    @Column(name = "WaypointID", index = true)
    public long id;

    @Column(name = "lat")
    public double latitude;

    @Column(name = "long")
    public double longitude;

    @Column(name = "question")
    public String question;

    @Column(name = "answers")
    public List<String> answers;

    @Column(name = "route")
    public GeoTownRoute route;

}
