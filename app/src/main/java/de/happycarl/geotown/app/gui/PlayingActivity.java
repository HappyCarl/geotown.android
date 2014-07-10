package de.happycarl.geotown.app.gui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.gui.views.WaypointDistanceView;

public class PlayingActivity extends Activity implements SeekBar.OnSeekBarChangeListener{


    @InjectView(R.id.seekBar)
    SeekBar bar;

    @InjectView(R.id.distance_view)
    WaypointDistanceView waypointDistanceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);

        ButterKnife.inject(this);
        bar.setOnSeekBarChangeListener(this);
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

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        waypointDistanceView.setDistance(progress);
        Log.d("Distance", "distance changed to " + progress);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
