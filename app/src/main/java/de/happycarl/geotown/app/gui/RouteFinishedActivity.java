package de.happycarl.geotown.app.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.activeandroid.query.Select;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.models.GeoTownRoute;
import de.happycarl.geotown.app.models.GeoTownWaypoint;

public class RouteFinishedActivity extends Activity {

    private GoogleMap mMap;
    private GeoTownRoute mRoute;

    @InjectView(R.id.route_finished_detail)
    TextView mRouteDetailText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_finished);

        ButterKnife.inject(this);

        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.route_finished_map)).getMap();

        Long routeId = getIntent().getExtras().getLong("routeId");
        new LoadRouteTask().execute(routeId);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.route_finished, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        okay();
    }

    @OnClick(R.id.route_finished_button_okay)
    void okay() {
        Intent overview = new Intent(this, OverviewActivity.class);
        startActivity(overview);
        finish();
    }

    private void updateRouteUI() {
        mRouteDetailText.setText(getString(R.string.text_route_finished_text, mRoute.name));

        for(GeoTownWaypoint w : mRoute.waypoints())
            mMap.addMarker(new MarkerOptions().position(new LatLng(w.latitude, w.longitude)).title(w.question));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mRoute.latitude, mRoute.longitude), 13));
    }

    private class LoadRouteTask extends AsyncTask<Long, Void, GeoTownRoute> {

        @Override
        protected void onPostExecute(GeoTownRoute geoTownRoute) {
            mRoute = geoTownRoute;
            updateRouteUI();
        }

        @Override
        protected GeoTownRoute doInBackground(Long... longs) {
            return new Select()
                    .from(GeoTownRoute.class)
                    .where("routeID = ?", longs[0])
                    .limit(1)
                    .executeSingle();
        }
    }

}
