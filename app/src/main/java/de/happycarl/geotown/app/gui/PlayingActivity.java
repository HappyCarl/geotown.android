package de.happycarl.geotown.app.gui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.gui.views.FadingImageView;
import de.happycarl.geotown.app.gui.views.WaypointDistanceView;
import de.happycarl.geotown.app.models.GeoTownWaypoint;
import de.happycarl.geotown.app.service.GameService;

public class PlayingActivity extends SystemBarTintActivity{


    @InjectView(R.id.distance_view)
    WaypointDistanceView waypointDistanceView;

    @InjectView(R.id.waypointImage)
    FadingImageView imageView;

    Messenger gameService = null;
    boolean isBound = false;

    GeoTownWaypoint currentWaypoint;

    private class IncomingHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            Log.d("ClientReceiver" , "Received " + msg.what + " (" + msg.arg1 + ";" + msg.arg2 + ")");
            switch (msg.what) {
                case GameService.MSG_DISTANCE_TO_TARGET:
                    waypointDistanceView.setDistance(msg.arg1);
                    break;
                case GameService.MSG_NEW_WAYPOINT:
                    newCurrentWaypoint(GeotownApplication.intsToLong(msg.arg1, msg.arg2));
                    break;
                case GameService.MSG_TARGET_WAYPOINT_REACHED:
                    showWaypointQuestion();
                    break;
                case GameService.MSG_ERROR:
                    switch (msg.arg1) {
                        case GameService.ERROR_NO_ROUTE:
                            doUnbindService();
                            break;
                        default:
                    }
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void showWaypointQuestion() {
        //TODO: SHOW QUESTION
        Log.d("showWaypointQuestion", "Question showing: " + currentWaypoint.question + ": \n"
                +currentWaypoint.rightAnswer + "\n" + currentWaypoint.wrongAnswers);
        sendMessage(GameService.MSG_QUESTION_ANSWERED, 0, 0);
    }


    final Messenger messenger = new Messenger(new IncomingHandler());

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gameService = new Messenger(service);
            Log.d("GameService","Attached to service");

            sendMessage(GameService.MSG_REGISTER_CLIENT, 0, 0);
            sendMessage(GameService.MSG_SET_LOCATION_MODE, GameService.ListenMode.FOREGROUND.ordinal(), 0);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("GameService", "Disconnected service");
            gameService = null;

            Toast.makeText(PlayingActivity.this, R.string.service_connection_lost, Toast.LENGTH_LONG).show();
        }
    };

    private void doBindService() {
        bindService(new Intent(PlayingActivity.this, GameService.class), serviceConnection, BIND_AUTO_CREATE);

        isBound = true;
        Log.d("GameService", "Bound to service");


    }

    private void doUnbindService() {
        if(isBound) {
            if(gameService != null) {
                sendMessage(GameService.MSG_UNREGISTER_CLIENT, 0 ,0 );
            }

            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);

        ButterKnife.inject(this);

        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setFadeDirection(FadingImageView.FadeSide.BOTTOM_SIDE);
        imageView.setEdgeLength(30);

        doBindService();


    }

    @Override
    public void onResume() {
        sendMessage(GameService.MSG_SET_LOCATION_MODE, GameService.ListenMode.FOREGROUND.ordinal(), 0);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.playing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop() {
        sendMessage(GameService.MSG_SET_LOCATION_MODE, GameService.ListenMode.BACKGROUND.ordinal(), 0);
        super.onStop();
    }

    private void newCurrentWaypoint(long id) {
        Log.d("newCurrentWaypoint", "ID: " + id);
        if(id == -1L) {
            //Somewhat error handling here

            return;
        } else if(id == -2L) {
            //route finished, go back to overview and stop service
            doUnbindService();
            Toast.makeText(this, R.string.route_finished, Toast.LENGTH_LONG).show();
            Intent overview = new Intent(this, OverviewActivity.class);
            startActivity(overview);
            finish();
            return;
        }
        currentWaypoint = new Select()
                .from(GeoTownWaypoint.class)
                .where("WaypointID = ?", id)
                .executeSingle();
        if(currentWaypoint != null) {
            Log.d("newCurrentWaypoint", "URL: " + currentWaypoint.imageURL);

            Picasso.with(this)
                    .load(currentWaypoint.imageURL)
                    .error(R.drawable.ic_launcher)
                    .into(imageView);

            sendMessage(GameService.MSG_DISTANCE_TO_TARGET, 0, 0);
        }


    }

    private void sendMessage(int request, int arg1, int arg2) {
        if(gameService == null)
            return;
        Log.d("ClientMessenger", "Sending :" + request + " (" + arg1 + ";"+arg2+")");
        try {
            Message msg = Message.obtain(null, request, arg1, arg2);
            msg.replyTo = messenger;
            gameService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            doUnbindService();
            Toast.makeText(this, "Service did not respond", Toast.LENGTH_LONG).show();
        }
    }

}
