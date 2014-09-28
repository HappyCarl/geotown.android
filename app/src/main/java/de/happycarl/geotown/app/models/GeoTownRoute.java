package de.happycarl.geotown.app.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.List;

/**
 * Created by ole on 20.06.14.
 */

@Table(name = "Routes")
public class GeoTownRoute extends Model {

    @Column(name = "name", index = true)
    public String name;

    @Column(name = "owner")
    public String owner;

    @Column(name = "routeID", index = true, unique = true)
    public long id;

    @Column(name = "lat")
    public double latitude;

    @Column(name = "long")
    public double longitude;

    @Column(name = "mine")
    public boolean mine;

    @Column(name = "nearIndex")
    public int nearIndex;

    @Column(name = "starred")
    public boolean starred;

    @Column(name = "location")
    public String location;

    public List<GeoTownWaypoint> waypoints() {
        return getMany(GeoTownWaypoint.class, "route");
    }

}
