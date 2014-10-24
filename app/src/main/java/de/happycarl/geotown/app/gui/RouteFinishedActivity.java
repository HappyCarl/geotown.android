package de.happycarl.geotown.app.gui;

import android.app.Activity;
import android.widget.TextView;

import com.activeandroid.query.Select;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.models.GeoTownRoute;
import de.happycarl.geotown.app.models.GeoTownWaypoint;

@EActivity(R.layout.activity_route_finished)
@OptionsMenu(R.menu.route_finished)
public class RouteFinishedActivity extends Activity {

    private GoogleMap mMap;
    private GeoTownRoute mRoute;

    @ViewById(R.id.route_finished_detail)
    TextView mRouteDetailText;

    @AfterViews
    protected void afterViews() {
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.route_finished_map)).getMap();

        Long routeId = getIntent().getExtras().getLong("routeId");
        loadRoute(routeId);
    }

    @Override
    public void onBackPressed() {
        okay();
    }

    @Click(R.id.route_finished_button_okay)
    void okay() {
        OverviewActivity_.intent(this).start();
        finish();
    }

    @UiThread
    protected void updateRouteUI() {
        mRouteDetailText.setText(getString(R.string.text_route_finished_text, mRoute.name));

        for(GeoTownWaypoint w : mRoute.waypoints())
            mMap.addMarker(new MarkerOptions().position(new LatLng(w.latitude, w.longitude)).title(w.question));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mRoute.latitude, mRoute.longitude), 13));
    }

    @Background
    protected void loadRoute(long routeId) {
        mRoute = new Select()
                .from(GeoTownRoute.class)
                .where("routeID = ?", routeId)
                .limit(1)
                .executeSingle();
        updateRouteUI();
    }

}
