package de.happycarl.geotown.app.gpx;

import android.location.Location;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.models.GeoTownRoute;
import de.happycarl.geotown.app.models.GeoTownWaypoint;

/**
 * Created by ole on 28.09.14.
 */
public class GPXRouteLogger {

    GeoTownRoute route;

    Map<GeoTownWaypoint, Date> waypointsInOrder = new HashMap<>();

    Map<Location, Date> position = new HashMap<>();

    String username;

    DateFormat dateFormat;

    public GPXRouteLogger() {
        username = GeotownApplication.getPreferences().getString(AppConstants.PREF_ACCOUNT_NAME, "<default>");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        dateFormat.setTimeZone(tz);

    }

    public GPXRouteLogger(GeoTownRoute r) {
        this();
        route = r;

    }

    public boolean setRoute(GeoTownRoute r) {
        if (route == null) {
            route = r;
            return true;
        }
        return false;
    }

    public void addWaypoint(GeoTownWaypoint wp) {
        waypointsInOrder.put(wp, new Date());
    }

    public void addPosition(Location pos) {
        position.put(pos, new Date());
    }

    public void generateXml() {
        if (route == null)
            return;

        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();

        try {
            serializer.setOutput(writer);
            serializer.startDocument("UFT-8", false);

            serializer.startTag("", "gpx"); //Root element
            serializer.attribute("", "version", "1.1");
            serializer.attribute("", "creator", "GeoTown");
            serializer.attribute("", "xmlns", "http://www.topografix.com/GPX/1/1");

            generateMetadata(serializer);

            generateRouteData(serializer);

            generateGPSTrack(serializer);

            serializer.endTag("", "gpx");

            serializer.endDocument();


            if (!saveFileToSDCard(writer)) return;

        } catch (Exception e) {
            Log.d("GPXRouteLogger", e.getMessage());
        }

    }

    private boolean saveFileToSDCard(StringWriter writer) throws IOException {
        if(!isExternalStorageWritable()) {
            Log.e("GPXRouteLogger", "External storage not writable");
            return false;

        }
        File trackFileDirectory = new File(Environment.getExternalStorageDirectory(), "/GeoTown");
        Log.d("GPXRouteLogger", "" + trackFileDirectory.exists());
        if (!trackFileDirectory.exists() && !trackFileDirectory.mkdirs()) {
            Log.e("GPXRouteLogger", "Directory creation failed: " + Environment.getExternalStorageDirectory().getAbsolutePath() + "   -   " + trackFileDirectory.getAbsolutePath());
            return false;
        }

        File trackFile = new File(trackFileDirectory, "route-" + route.name + "-" + route.id + "-" + dateFormat.format(new Date()) + ".gpx");

        FileWriter fileWriter = new FileWriter(trackFile);
        fileWriter.write(writer.toString());
        fileWriter.close();
        return true;
    }

    private void generateGPSTrack(XmlSerializer serializer) throws IOException {
        serializer.startTag("", "trk"); //beginning of actual position data

        serializer.startTag("", "name");
        serializer.text(route.name + " - " + username);
        serializer.endTag("", "name");

        serializer.startTag("", "desc");
        serializer.text("GPS data of route \"" + route.name + "\" played by \"" + username + "\"");
        serializer.endTag("", "desc");

        serializer.startTag("", "trkseg");

        for (Map.Entry<Location, Date> entry : position.entrySet()) {
            serializer.startTag("", "trkpt");
            serializer.attribute("", "lat", Double.toString(entry.getKey().getLatitude()).replace(',', '.'));
            serializer.attribute("", "lon", Double.toString(entry.getKey().getLongitude()).replace(',', '.'));

            serializer.startTag("", "time");
            serializer.text(dateFormat.format(entry.getValue()));
            serializer.endTag("", "time");

            if (entry.getKey().hasAltitude()) {
                serializer.startTag("", "ele");
                serializer.text(Double.toString(entry.getKey().getAltitude()));
                serializer.endTag("", "ele");
            }

            serializer.endTag("", "trkpt");
        }

        serializer.endTag("", "trkseg");

        serializer.endTag("", "trk");
    }

    private void generateRouteData(XmlSerializer serializer) throws IOException {
        serializer.startTag("", "rte"); //Here will be the route data containing the waypoints in the order played by the user

        serializer.startTag("", "name");
        serializer.text(route.name);
        serializer.endTag("", "name");

        for (Map.Entry<GeoTownWaypoint, Date> entry : waypointsInOrder.entrySet()) {

            serializer.startTag("", "rtept");
            serializer.attribute("", "lat", Double.toString(entry.getKey().latitude).replace(',', '.'));
            serializer.attribute("", "lon", Double.toString(entry.getKey().longitude).replace(',', '.'));

            serializer.startTag("", "time");
            serializer.text(dateFormat.format(entry.getValue()));
            serializer.endTag("", "time");

            serializer.startTag("", "name");
            serializer.text(Long.toString(entry.getKey().id));
            serializer.endTag("", "name");

            serializer.startTag("", "desc");
            serializer.text(entry.getKey().imageURL);
            serializer.endTag("", "desc");

            serializer.endTag("", "rtept");
        }

        serializer.endTag("", "rte"); //end of route data
    }

    private void generateMetadata(XmlSerializer serializer) throws IOException {
        serializer.startTag("", "metadata"); //Child of gpx

        serializer.startTag("", "desc");
        serializer.text("GPS logging data of GeoTown route " + route.name + " (id: " + route.id + ")");
        serializer.endTag("", "desc");

        serializer.startTag("", "author");
        serializer.startTag("", "name");
        serializer.text(username);
        serializer.endTag("", "name");
        serializer.endTag("", "author");

        serializer.endTag("", "metadata"); //end of metadata
    }

    /* Checks if external storage is available for read and write
    * Taken from https://developer.android.com/training/basics/data-storage/files.html*/
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
