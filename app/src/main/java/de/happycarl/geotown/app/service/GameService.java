package de.happycarl.geotown.app.service;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.activeandroid.query.Select;

import java.util.ArrayList;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.gui.PlayingActivity;
import de.happycarl.geotown.app.models.GeoTownRoute;
import de.happycarl.geotown.app.models.GeoTownWaypoint;

/**
 * Created by ole on 12.07.14.
 */
public class GameService extends Service{

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
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
    public static final int MSG_NEW_WAYPOINT = 0x5;

    public static final int MSG_SET_LOCATION_MODE = 0x6;

    public static final int MSG_QUESTION_ANSWERED = 0x7;

    public static final int MSG_ERROR = 0x99;;
        public static final int ERROR_NO_ROUTE = 0x1;

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    //Stuff for service
    NotificationManager mNM;
    ArrayList<Messenger> mClients = new ArrayList<>();


    private class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Log.d("ServerReceiver" , "Received " + msg.what + " (" + msg.arg1 + ";" + msg.arg2 + ")");
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_DISTANCE_TO_TARGET:
                    reportDistanceToTarget();
                    break;
                case MSG_SET_LOCATION_MODE:
                    if(msg.arg1 == ListenMode.BACKGROUND.ordinal()) {
                        setLocationListenMode(ListenMode.BACKGROUND);
                    } else if(msg.arg1 == ListenMode.FOREGROUND.ordinal()){
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



    //Location stuff
    LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateDistanceToTarget(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };



    public enum ListenMode {
        FOREGROUND, BACKGROUND, NONE
    }



//--------------------------------------------------------------------------------------------------
//GAME LOGIC VARIABLES
//--------------------------------------------------------------------------------------------------
    private int distanceToTarget = 0;
    private GeoTownRoute currentRoute;
    private GeoTownWaypoint currentWaypoint;
    private Location currentTarget;


//--------------------------------------------------------------------------------------------------
//SERVICE LIFECYCLE & COMMUNICATION
//--------------------------------------------------------------------------------------------------
    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);



        loadRoute();

        //Normally set to background, activity should set it to foreground
        setLocationListenMode(ListenMode.BACKGROUND);

    }

    @Override
    public void onDestroy() {
        mNM.cancel(R.string.remote_service_notification);

        locationManager.removeUpdates(locationListener);
    }


    private void showStatusNotification() {
        CharSequence text = getText(R.string.currently_playing);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_marker)
                .setContentTitle(text)
                .setContentText("You are playing route '" + currentRoute.name + "'");
        Intent intent = new Intent(this, PlayingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);

        mNM.notify(R.string.remote_service_notification, builder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void sendMessage(int messageCode, int arg1, int arg2) {
        Log.d("ServerMessenger", "Sending :" + messageCode + " (" + arg1 + ";"+arg2+")");
        for(int i = mClients.size(); i >= 0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null,messageCode, arg1, arg2));
            } catch (RemoteException ex) {
                mClients.remove(i);
            }
        }
    }

    private void reportDistanceToTarget() {
        sendMessage(MSG_DISTANCE_TO_TARGET, distanceToTarget, 0);
    }

    private void reportError(int errorCode) {
        sendMessage(MSG_ERROR, errorCode, 0);
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
        if(currentRoute == null) {
            reportError(ERROR_NO_ROUTE);
            stopSelf();
        } else {
            if(!loadCurrentWaypoint()) { //Old waypoint loaded, so report it to app
                int[] routeid = GeotownApplication.longToInts(currentWaypoint.id);
                sendMessage(MSG_NEW_WAYPOINT, routeid[0], routeid[1]);
            };
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

        for(GeoTownWaypoint w : currentRoute.waypoints()) {
            if(w.id == id) {
                currentWaypoint = w;
                if(currentWaypoint.done) {
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
    }

    private void selectNewWaypoint() {
        if(currentWaypoint.done) {
            currentWaypoint = new Select()
                    .from(GeoTownWaypoint.class)
                    .where("done = ?", false)
                    .orderBy("RANDOM()")
                    .executeSingle();
            if(currentWaypoint == null) {
                //we finished the route
                GeotownApplication.getPreferences().edit()
                        .putLong(AppConstants.PREF_CURRENT_WAYPOINT, -1L);
                int[] id = GeotownApplication.longToInts(-2L);
                sendMessage(MSG_NEW_WAYPOINT, id[0], id[1]);

            }
            GeotownApplication.getPreferences().edit()
                    .putLong(AppConstants.PREF_CURRENT_WAYPOINT, currentWaypoint.id);
            setLocationToWaypoint();

            int[] id = GeotownApplication.longToInts(currentWaypoint.id);
            sendMessage(MSG_NEW_WAYPOINT, id[0], id[1]);
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
    private void updateDistanceToTarget(Location location) {
        if(location == null)
            return;
        distanceToTarget = (int) currentTarget.distanceTo(location);
        if(distanceToTarget <= AppConstants.WAYPOINT_RADIUS) {
            sendMessage(MSG_TARGET_WAYPOINT_REACHED, distanceToTarget, 0);

        }
        reportDistanceToTarget();
    }

    public void setLocationListenMode(ListenMode mode) {
        //First remove it
        locationManager.removeUpdates(locationListener);
        Log.d("LocationService", "Disabled location updates");
        switch (mode) {
            case FOREGROUND:
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Log.d("LocationService", "Set to GPS mode");
                break;
            case BACKGROUND:
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30, 100, locationListener);
                Log.d("LocationService", "Set to Network mode");
                break;
            default:
        }
    }


}
