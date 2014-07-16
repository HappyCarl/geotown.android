package de.happycarl.geotown.app.gui;

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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.activeandroid.query.Select;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.gui.views.FadingImageView;
import de.happycarl.geotown.app.gui.views.WaypointDistanceView;
import de.happycarl.geotown.app.models.GeoTownWaypoint;
import de.happycarl.geotown.app.service.GameService;
import de.happycarl.geotown.app.util.GameUtil;
import de.happycarl.geotown.app.util.MathUtil;

public class PlayingActivity extends SystemBarTintActivity{


    public static final int SERVICE_CONNECTION_ID = R.string.service_connection_id;

    @InjectView(R.id.distance_view)
    WaypointDistanceView waypointDistanceView;

    @InjectView(R.id.waypointImage)
    FadingImageView imageView;

    @InjectView(R.id.viewswitch_playing)
    ViewFlipper switcher;

    @InjectView(R.id.answer1)
    Button answer1;

    @InjectView(R.id.answer2)
    Button answer2;

    @InjectView(R.id.answer3)
    Button answer3;

    @InjectView(R.id.answer4)
    Button answer4;

    @InjectView(R.id.questionText)
    TextView questionText;

    Messenger gameService = null;
    boolean isBound = false;

    GeoTownWaypoint currentWaypoint;

    private boolean serviceIntoBackgroundMode = true;

    private class IncomingHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            Log.d("ClientReceiver" , "Received " + msg.what + " (" + msg.arg1 + ";" + msg.arg2 + ")");
            switch (msg.what) {
                case GameService.MSG_DISTANCE_TO_TARGET:
                    waypointDistanceView.setDistance(msg.arg1);
                    break;
                case GameService.MSG_NEW_WAYPOINT:
                    newCurrentWaypoint(MathUtil.intsToLong(msg.arg1, msg.arg2));
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


    final Messenger messenger = new Messenger(new IncomingHandler());

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gameService = new Messenger(service);
            Log.d("GameService","Attached to service");

            sendMessage(GameService.MSG_REGISTER_CLIENT, SERVICE_CONNECTION_ID, 0);
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
                sendMessage(GameService.MSG_UNREGISTER_CLIENT, SERVICE_CONNECTION_ID ,0 );
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

        doBindService();

        imageView.setFadeDirection(FadingImageView.FadeSide.BOTTOM_SIDE);
        imageView.setEdgeLength(30);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);



    }

    private void fillQuestion() {
        if(currentWaypoint != null) {
            Log.d("WaypointQuestion",currentWaypoint.question + ": " + currentWaypoint.rightAnswer + " : " + currentWaypoint.wrongAnswers);
            questionText.setText("OLAF WAS HERE");

            ArrayList<String> ans = new ArrayList<>();
            ans.add(currentWaypoint.rightAnswer);
            String[] wrongAns = currentWaypoint.wrongAnswers.split("|");
            for (String wrongAn1 : wrongAns) {
                Log.d("WaypointQuestion", wrongAn1);
            }
            int ansCount = 1;
            for (String wrongAn : wrongAns) {
                if (!wrongAn.isEmpty()) {
                    ans.add(wrongAn);
                    ansCount++;
                }
                if (ansCount == 4)
                    break;
            }

            Collections.shuffle(ans);

            try{
                answer1.setText(ans.get(0));
                answer2.setText(ans.get(1));
                answer3.setText(ans.get(2));
                answer4.setText(ans.get(3));
            } catch (NullPointerException ex) {
                Log.d("WaypointQuestion", "Creator did not specify 4 answers");
            }

        }

    }



    @Override
    public void onStart() {
        super.onStart();
        GeotownApplication.getGameHelper().onStart(this);
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
        if (id == R.id.action_cancel) {
            Log.d("PlayingActivity","User requested route cancel");
            routeEnd(false);
            return true;
        } else if(id == R.id.action_switch) {
            switcher.showNext();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop() {
        if(serviceIntoBackgroundMode)
            sendMessage(GameService.MSG_SET_LOCATION_MODE, GameService.ListenMode.BACKGROUND.ordinal(), 0);
        GeotownApplication.getGameHelper().onStop();

        super.onStop();
    }


    private void newCurrentWaypoint(long id) {
        Log.d("newCurrentWaypoint", "ID: " + id);
        if(id == -1L) {
            //Somewhat error handling here

            return;
        } else if(id == -2L) {
            routeEnd(true);
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

            fillQuestion();

            sendMessage(GameService.MSG_DISTANCE_TO_TARGET, 0, 0);
        }


    }

    private void showWaypointQuestion() {
        //TODO: SHOW QUESTION
        Log.d("showWaypointQuestion", "Question showing: " + currentWaypoint.question + ": \n"
                +currentWaypoint.rightAnswer + "\n" + currentWaypoint.wrongAnswers);
        if(switcher.getCurrentView().getId() == R.id.searchLayout)
            switcher.showNext();

    }

    private void questionAnswerCorrect() {
        sendMessage(GameService.MSG_QUESTION_ANSWERED, 0, 0);
        GameUtil.publishWaypointFinishToPlayGames(this);

        if(switcher.getCurrentView().getId() == R.id.questionLayout)
            switcher.showNext();
    }

    private void routeEnd(boolean finished) {
        //route finished, go back to overview and stop service
        serviceIntoBackgroundMode = false;
        doUnbindService();
        stopService(new Intent(PlayingActivity.this, GameService.class));
        if(finished) {
            GameUtil.publishRouteFinishToPlayGames(this);
            Toast.makeText(this, R.string.route_finished, Toast.LENGTH_LONG).show();
        }

        Intent overview = new Intent(this, OverviewActivity.class);
        startActivity(overview);
        finish();
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

    @OnClick(R.id.answer1)
    public void onAnswer1Clicked() {
        if(answer1.getText().toString().equals(currentWaypoint.rightAnswer)) {
            questionAnswerCorrect();
        }
    }

    @OnClick(R.id.answer2)
    public void onAnswer2Clicked() {
        if(answer2.getText().toString().equals(currentWaypoint.rightAnswer)) {
            questionAnswerCorrect();
        }
    }

    @OnClick(R.id.answer3)
    public void onAnswer3Clicked() {
        if(answer3.getText().toString().equals(currentWaypoint.rightAnswer)) {
            questionAnswerCorrect();
        }
    }

    @OnClick(R.id.answer4)
    public void onAnswer4Clicked() {
        if(answer4.getText().toString().equals(currentWaypoint.rightAnswer)) {
            questionAnswerCorrect();
        }
    }

}
