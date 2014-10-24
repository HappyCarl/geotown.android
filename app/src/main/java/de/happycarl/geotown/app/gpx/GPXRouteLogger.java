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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
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

    final Map<GeoTownWaypoint, Date> waypointsInOrder = new HashMap<>();

    Map<Location, Date> position = new HashMap<>();

    final String username;

    final DateFormat dateFormat;

    Location lastLocation;

    public GPXRouteLogger() {
        username = GeotownApplication.getPreferences().getString(AppConstants.PREF_ACCOUNT_EMAIL, "<no-mail-given>");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(tz);

    }

    public GPXRouteLogger(GeoTownRoute r) {
        this();
        route = r;

    }

    public void addWaypoint(GeoTownWaypoint wp) {
        waypointsInOrder.put(wp, new Date());
        Log.d("GPXRouteLogger", "Added waypoint : " + wp.question);
    }

    public void addPosition(Location pos) {
        if(lastLocation == null) {
            lastLocation = pos;
            lastLocation.setLatitude(0);
        }
        //only log new position data
        if(pos.getLongitude() == lastLocation.getLongitude() && pos.getLatitude() == lastLocation.getLatitude())
            return;

        position.put(pos, new Date());
        lastLocation = pos;
        Log.d("GPXRouteLogger", "Added position: " + pos.getLatitude() + " : " + pos.getLongitude());
    }

    public String generateXml() {
        if (route == null)
            return null;

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


            return saveFileToSDCard(writer);

        } catch (Exception e) {
            Log.e("GPXRouteLogger", e.getMessage());
        }
        return null;

    }

    private String saveFileToSDCard(StringWriter writer) throws IOException {
        if(!isExternalStorageWritable()) {
            Log.e("GPXRouteLogger", "External storage not writable");
            return null;

        }
        File trackFileDirectory = new File(Environment.getExternalStorageDirectory(), "/GeoTown");
        Log.d("GPXRouteLogger", "" + trackFileDirectory.exists());
        if (!trackFileDirectory.exists() && !trackFileDirectory.mkdirs()) {
            Log.e("GPXRouteLogger", "Directory creation failed: " + Environment.getExternalStorageDirectory().getAbsolutePath() + "   -   " + trackFileDirectory.getAbsolutePath());
            return null;
        }

        File trackFile = new File(trackFileDirectory, "route-" + route.name + "-" + route.id + "-" + dateFormat.format(new Date()) + ".gpx");

        FileWriter fileWriter = new FileWriter(trackFile);
        fileWriter.write(writer.toString());
        fileWriter.close();
        return trackFile.getAbsolutePath();
    }

    static <K, V> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return ((Comparable<V>) o1.getValue())
                        .compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
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

        position = sortByValue(position);

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
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
