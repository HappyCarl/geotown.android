package de.happycarl.geotown.app.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
//import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.activeandroid.query.Select;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.common.collect.HashBiMap;
import com.squareup.otto.Subscribe;

import java.util.List;
import java.util.Random;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.BuildConfig;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.requests.FinishTrackRequest;
import de.happycarl.geotown.app.api.requests.StartTrackRequest;
import de.happycarl.geotown.app.events.net.TrackFinishedEvent;
import de.happycarl.geotown.app.events.net.TrackStartedEvent;
import de.happycarl.geotown.app.gpx.GPXRouteLogger;
import de.happycarl.geotown.app.gui.PlayingActivity;
import de.happycarl.geotown.app.models.GeoTownRoute;
import de.happycarl.geotown.app.models.GeoTownWaypoint;
import de.happycarl.geotown.app.util.MathUtil;

/**
 * Created by ole on 12.07.14.
 */
public class GameService extends Service implements GoogleApiClient.ConnectionCallbacks, com.google.android.gms.location.LocationListener, GoogleApiClient.OnConnectionFailedListener, SensorEventListener {

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent. The 2nd argument has to be the seed for the
     * PRNG
     */
    public static final int MSG_REGISTER_CLIENT = 0x1;
    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 0x2;
    /**
     * Command the service to report the current distance to the current target
     */
    public static final int MSG_DISTANCE_TO_TARGET = 0x3;
    /**
     * Sent to the clients if the current waypoint was reached
     */
    public static final int MSG_TARGET_WAYPOINT_REACHED = 0x4;
    /**
     * Sent to the clients once a new waypoint is set, containing the id as payload
     */
    public static final int MSG_NEW_WAYPOINT = 0x5;

    /**
     * Sent by client to switch from GPS to wifi location mode or vice versa
     */
    public static final int MSG_SET_LOCATION_MODE = 0x6;

    /**
     * Sent by client if the question was answered correctly,
     * indicating that a new waypoint has to be generated.
     */
    public static final int MSG_QUESTION_ANSWERED = 0x7;

    /**
     * Sent by Service when init is done
     */
    public static final int MSG_CONNECTED = 0x8;

    /*
     * Sent by activity when it was attached
     */
    public static final int MSG_ATTACHED = 0x9;


    public static final int MSG_ERROR = 0x99;

    public static final int ERROR_NO_ROUTE = 0x1;

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    //Stuff for service
    NotificationManager mNM;
    HashBiMap<Integer, Messenger> mClients = HashBiMap.create();

    private Random random;

    GPXRouteLogger routeLogger;

    private long trackId;

    private SensorManager sensorManager;

    private float[] mGravs = new float[3];
    private float[] mGeoMags = new float[3];
    private float[] mOrientation = new float[3];
    private float[] mRotationM = new float[9];

    private Location currentLocation;

    private class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Log.d("ServerReceiver", "Received " + msg.what + " (" + msg.arg1 + ";" + msg.arg2 + ")");
            switch (msg.what) {
                case MSG_ATTACHED:
                    if (mApiClient.isConnected())
                        GameService.this.sendMessage(MSG_CONNECTED, 0, 0);
                    break;
                case MSG_REGISTER_CLIENT:
                    Log.d("GameService", "Client registered :" + msg.arg1);
                    if (msg.arg1 == 0)
                        break;
                    mClients.forcePut(msg.arg1, msg.replyTo);
                    reportWaypoint();
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.d("GameService", "Client unregistered :" + msg.arg1);
                    if (msg.arg1 == 0)
                        break;
                    mClients.remove(msg.arg1);
                    if (mClients.size() == 0)
                        stopSelf();
                    break;
                case MSG_DISTANCE_TO_TARGET:
                    reportDistanceToTarget();
                    break;
                case MSG_SET_LOCATION_MODE:
                    if (msg.arg1 == ListenMode.BACKGROUND.ordinal()) {
                        setLocationListenMode(ListenMode.BACKGROUND);
                    } else if (msg.arg1 == ListenMode.FOREGROUND.ordinal()) {
                        setLocationListenMode(ListenMode.FOREGROUND);
                    } else {
                        setLocationListenMode(ListenMode.NONE);
                    }
                    break;
                case MSG_QUESTION_ANSWERED:
                    questionAswered();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private GoogleApiClient mApiClient;


    public enum ListenMode {
        FOREGROUND, BACKGROUND, NONE
    }

    private ListenMode currentListenMode;
    NotificationCompat.Builder notificationBuilder;


    //--------------------------------------------------------------------------------------------------
    //GAME LOGIC VARIABLES
    //--------------------------------------------------------------------------------------------------
    private int distanceToTarget = -1;
    private int compassDirection;
    private GeoTownRoute currentRoute;
    private GeoTownWaypoint currentWaypoint;
    private Location currentTarget;


    //--------------------------------------------------------------------------------------------------
    //SERVICE LIFECYCLE & COMMUNICATION
    //--------------------------------------------------------------------------------------------------
    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


        mApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient.connect();

        GeotownApplication.getEventBus().register(this);

        loadRoute();

        long seed = GeotownApplication.getPreferences().getLong(AppConstants.PREF_PRNG_SEED, 0);
        Log.d("Seed", "seed is " + seed + "  : " + (seed != 0L));

        if(seed == 0L) {
            seed = System.currentTimeMillis();
            GeotownApplication.getPreferences().edit().putLong(AppConstants.PREF_PRNG_SEED, seed).apply();
            Log.d("seed", "put seed " + seed);
        }
        random = new Random(seed);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        Log.d("GameService", "Shutting down...");
        mNM.cancel(AppConstants.REMOTE_SERVICE_NOTIFICATION);
        mNM.cancel(AppConstants.REMOTE_SERVICE_NOTIFICATION + 1);
        LocationServices.FusedLocationApi.removeLocationUpdates(mApiClient, this);
        //sensorManager.unregisterListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        //sensorManager.unregisterListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
    }


    private void showStatusNotification() {
        if(currentListenMode == ListenMode.FOREGROUND) return;
        /**
        if(currentListenMode == ListenMode.BACKGROUND)
            showPhoneNotification();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    showWearableNotification();
                }
            }
        }).start();**/
        CharSequence text = getText(R.string.text_overview_currently_playing);
        CharSequence distText = getText(R.string.text_playing_distance_to);

        Log.d("ROUTE", currentRoute + "");

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_world)
                .setContentTitle(text + " '" + currentRoute.name + "'")
                .setContentText(distText + " " + distanceToTarget + "m")
                .setOngoing(true);
        Intent intent = new Intent(this, PlayingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);


        mNM.notify(AppConstants.REMOTE_SERVICE_NOTIFICATION, notificationBuilder.build());
    }

    CharSequence wearDistText = "";

    /*private void showWearableNotification() {
        CharSequence text = getText(R.string.wear_notification_distance)  + " " + distanceToTarget + "m";
        CharSequence distText = currentRoute.name;

        if(distText.equals(wearDistText)) return;

        Bitmap bigPicture = null;
        try {
            bigPicture = Picasso.with(GameService.this).load(currentWaypoint.imageURL).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final NotificationCompat.Builder wearableNotificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_world)
                .setContentTitle(text)
                .setContentText(distText)
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bigPicture))
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setGroup("GROUP")
                .setGroupSummary(false);

        mNM.notify(AppConstants.REMOTE_SERVICE_NOTIFICATION + 1, wearableNotificationBuilder.build());

        wearDistText = distText;
    }*/

    private void showPhoneNotification() {
        CharSequence text = getText(R.string.text_overview_currently_playing);
        CharSequence distText = getText(R.string.text_playing_distance_to);

        Log.d("ROUTE", currentRoute + "");


        NotificationCompat.Builder phoneNotificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_world)
                .setContentTitle(text + " '" + currentRoute.name + "'")
                .setContentText(distText + " " + distanceToTarget + "m")
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Log.d("GameService", "Using wear notification");
            phoneNotificationBuilder.setGroup("GROUP")
                    .setGroupSummary(true);
        }*/

        Intent intent = new Intent(this, PlayingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        phoneNotificationBuilder.setContentIntent(pendingIntent);


        mNM.notify(AppConstants.REMOTE_SERVICE_NOTIFICATION, phoneNotificationBuilder.build());
    }

    private void clearStatusNotification() {
        mNM.cancel(AppConstants.REMOTE_SERVICE_NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void sendMessage(int messageCode, int arg1, int arg2) {
        Log.d("ServerMessenger", "Sending :" + messageCode + " (" + arg1 + ";" + arg2 + ")");
        Log.d("ServerMessenger", "Clients: " + mClients.size());
        for (Messenger messenger : mClients.values()) {
            try {
                messenger.send(Message.obtain(null, messageCode, arg1, arg2));
            } catch (RemoteException ex) {
                mClients.inverse().remove(messenger);
            }
        }

    }

    private void reportDistanceToTarget() {
        if(currentTarget == null)
            return;
        if (distanceToTarget == -1) {
            Location oldGPS = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
            if (oldGPS != null) {
                distanceToTarget = (int) currentTarget.distanceTo(oldGPS);
            }

        }
        sendMessage(MSG_DISTANCE_TO_TARGET, distanceToTarget, compassDirection);
    }

    private void reportWaypoint() {
        if (currentWaypoint == null) {
            selectNewWaypoint();
            return;
        }
        int[] wayid = MathUtil.longToInts(currentWaypoint.id);
        sendMessage(MSG_NEW_WAYPOINT, wayid[0], wayid[1]);
        reportDistanceToTarget();
    }

    private void reportError() {
        sendMessage(MSG_ERROR, GameService.ERROR_NO_ROUTE, 0);
    }


    //--------------------------------------------------------------------------------------------------
    //GAME LOGIC
    //--------------------------------------------------------------------------------------------------

    private void loadRoute() {
        long id = GeotownApplication.getPreferences()
                .getLong(AppConstants.PREF_CURRENT_ROUTE, -1L);

        currentRoute = new Select()
                .from(GeoTownRoute.class)
                .where("routeID = ?", id)
                .executeSingle();
        if (currentRoute == null) {
            reportError();
            stopSelf();
        } else {

            routeLogger = new GPXRouteLogger(currentRoute);
            GeotownApplication.getJobManager().addJob(new StartTrackRequest(id));

            if (!loadCurrentWaypoint()) { //Old waypoint loaded, so report it to app
                reportWaypoint();
            }

            showStatusNotification();
        }

    }

    private boolean loadCurrentWaypoint() {
        long id = GeotownApplication.getPreferences()
                .getLong(AppConstants.PREF_CURRENT_WAYPOINT, -1L);
        if (id == -1L) {
            selectNewWaypoint();
            return true;
        }

        for (GeoTownWaypoint w : currentRoute.waypoints()) {
            if (w.id == id) {
                currentWaypoint = w;
                if (currentWaypoint.done) {
                    selectNewWaypoint();
                    return true;
                }
                setLocationToWaypoint();
            }
        }
        return false;
    }

    private void setLocationToWaypoint() {
        currentTarget = new Location("GeoTownWaypoint");
        currentTarget.setLatitude(currentWaypoint.latitude);
        currentTarget.setLongitude(currentWaypoint.longitude);

        //As this is called for every new waypoint, no matter if loaded from Preferences or randomly selected
        //we pass the waypoint here to the logger
        routeLogger.addWaypoint(currentWaypoint);

        distanceToTarget = -1;
    }

    private void selectNewWaypoint() {
        if (random == null || currentRoute == null)
            return;
        if (currentWaypoint == null || currentWaypoint.done) {
            //Random with seed testing area
            List<GeoTownWaypoint> waypoints = new Select()
                    .from(GeoTownWaypoint.class)
                    .where("done = ?", false)
                    .where("route = ?", currentRoute.getId())
                    .orderBy("RANDOM()")
                    .execute();

            Log.d("selectNewWaypoint", "new route null?: " + (currentWaypoint == null));

            if (waypoints.isEmpty()) {
                Log.d("selectNewWaypoint", "Route finished");

                int[] id = MathUtil.longToInts(-2L);
                sendMessage(MSG_NEW_WAYPOINT, id[0], id[1]);

                String gpxPath = routeLogger.generateXml();

                //only submit if user accepted
                if(GeotownApplication.getPreferences().getBoolean(AppConstants.PREF_SUBMIT_TRACK_DATA, false)) {
                    Log.d("GameService", "UPLOAD TRACK STARTED");
                    GeotownApplication.getJobManager().addJob(new FinishTrackRequest(trackId, gpxPath));
                } else {
                    Log.d("GameService", "NO TRACK UPLOAD");
                }
            } else {
                int index = Math.abs(random.nextInt() % waypoints.size());
                Log.d("seed", "index: " + index + ": " + waypoints.size());
                currentWaypoint = waypoints.get(index);
                currentWaypoint.save();
                Log.d("selectNewWaypoint", "new ID: " + currentWaypoint.id);
                setLocationToWaypoint();

                reportWaypoint();
            }
        }
    }

    private void questionAswered() {
        currentWaypoint.done = true;
        currentWaypoint.save();
        selectNewWaypoint();
    }


    //--------------------------------------------------------------------------------------------------
    //LOCATION HANDLING
    //--------------------------------------------------------------------------------------------------

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("LocationServer", "Connected");
        setLocationListenMode(ListenMode.BACKGROUND);
        sendMessage(MSG_CONNECTED, 0, 0);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        updateDistanceToTarget(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("LOCATION", connectionResult.toString());
    }

    private void updateDistanceToTarget(Location location) {
        if (location == null || currentWaypoint == null || currentTarget == null)
            return;
        if(isMockLocationEnabled()) {
            Log.d("GameService", "Mock locations enabled");
            distanceToTarget = -42;
        } else {
            distanceToTarget = (int) currentTarget.distanceTo(location);

            float compass = location.bearingTo(currentTarget) - location.getBearing();
            compassDirection = (int)compass;
            Log.d("Compass", compassDirection + "  " + compass + "   " + location.bearingTo(currentTarget) + " - " + location.getBearing());

            if (distanceToTarget <= AppConstants.WAYPOINT_RADIUS && distanceToTarget > 0) {
                sendMessage(MSG_TARGET_WAYPOINT_REACHED, distanceToTarget, 0);
            }
        }
        reportDistanceToTarget();

        routeLogger.addPosition(location);

        showStatusNotification();
    }

    private boolean isMockLocationEnabled() {
        return !BuildConfig.DEBUG && !Settings.Secure.getString(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
    }

    public void setLocationListenMode(ListenMode mode) {
        if (mode == currentListenMode) {
            return;
        }
        currentListenMode = mode;
        //First remove it
        LocationServices.FusedLocationApi.removeLocationUpdates(mApiClient, this);


        LocationRequest request = LocationRequest.create();

        Log.d("LocationService", "Disabled location updates");
        switch (mode) {
            case FOREGROUND:
                request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                request.setInterval(5000);
                LocationServices.FusedLocationApi.requestLocationUpdates(mApiClient, request, this);
                Log.d("LocationService", "Set to GPS mode");
                clearStatusNotification();
                break;
            case BACKGROUND:
                request.setPriority(LocationRequest.PRIORITY_LOW_POWER);
                request.setInterval(1000);
                LocationServices.FusedLocationApi.requestLocationUpdates(mApiClient, request, this);
                Log.d("LocationService", "Set to Network mode");
                showStatusNotification();
                break;
            default:
        }
    }

    @Subscribe
    public void onTrackStarted(TrackStartedEvent event) {
        trackId = event.trackId;
    }

    @Subscribe
    public void onTrackFinished(TrackFinishedEvent event) {
        if (event.success) {
            Log.d("GameService", "Upload of track successfull");
        } else {
            Log.e("GameService", "Upload of track failed");
        }
    }


    /*
    All this stuff found there: https://groups.google.com/forum/#!topic/android-beginners/V4pOfLn8klQ
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values,0,mGravs,0,3);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mGeoMags,0,3);
                break;
            default:
                return;
        }
        if(SensorManager.getRotationMatrix(mRotationM, null, mGravs, mGeoMags)) {
            SensorManager.getOrientation(mRotationM, mOrientation);

            if (currentLocation == null)
                return;
            GeomagneticField geomagneticField = new GeomagneticField(
                    (float) currentLocation.getLatitude(),
                    (float) currentLocation.getLongitude(),
                    (float) currentLocation.getAltitude(),
                    System.currentTimeMillis());

            float bearing = currentLocation.bearingTo(currentTarget);
            float heading = mOrientation[0] + geomagneticField.getDeclination();

            float compass = bearing - heading;
            Log.d("Bearing", compass + " : " + bearing + " : " + heading);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }




}
