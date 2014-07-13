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

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.gui.views.WaypointDistanceView;
import de.happycarl.geotown.app.service.GameService;

public class PlayingActivity extends SystemBarTintActivity{


    @InjectView(R.id.distance_view)
    WaypointDistanceView waypointDistanceView;

    @InjectView(R.id.waypointImage)
    ImageView imageView;

    Messenger gameService = null;
    boolean isBound = false;

    private class IncomingHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GameService.MSG_DISTANCE_TO_TARGET:
                    waypointDistanceView.setDistance(msg.arg1);
                    break;
                case GameService.MSG_NEW_WAYPOINT:
                    newCurrentWaypoint(GeotownApplication.intsToLong(msg.arg1, msg.arg2));
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



    final Messenger messenger = new Messenger(new IncomingHandler());

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gameService = new Messenger(service);
            Log.d("GameService","Attached to service");
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
                try {
                    Message msg = Message.obtain(null, GameService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = messenger;
                    gameService.send(msg);
                } catch (RemoteException ex) {
                    //Service crashed
                    Log.d("GameService", "Service crashed");
                }
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

    private void newCurrentWaypoint(long id) {
        //TODO: load stuff and display image
    }

}
