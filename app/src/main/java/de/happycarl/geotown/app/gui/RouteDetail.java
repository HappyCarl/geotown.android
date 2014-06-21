package de.happycarl.geotown.app.gui;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.requests.GetRouteWaypointsRequest;
import de.happycarl.geotown.app.events.GeoTownRouteRetrievedEvent;
import de.happycarl.geotown.app.events.RouteWaypointsReceivedEvent;
import de.happycarl.geotown.app.models.GeoTownRoute;

public class RouteDetail extends SystemBarTintActivity {

    private static final int ROUTE_REQ_ID = 425694594;

    private long routeId = -1;

    @InjectView(R.id.detail_route_name)
    TextView routeName;

    @InjectView(R.id.detail_route_owner)
    TextView routeOwner;

    @InjectView(R.id.detail_route_waypoints)
    TextView routeWaypoints;

    @InjectView(R.id.play_route)
    Button playRoute;

    MapFragment mMapFragment;

    GeoTownRoute mRoute;

    ShareActionProvider mShareActionProvider;

    boolean created = false;

    boolean waypointsLoaded = false;
    boolean routeRetrieved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detail);

        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);

        created = true;

        String path = "";
        if (getIntent().getData() != null && getIntent().getData().getPath() != null)
            path = getIntent().getData().getPath().replaceAll("[^\\d]", "");

        routeId = getIntent().getLongExtra("routeID", -1L);

        if (routeId == -1 && !path.isEmpty()) {
            try {
                routeId = Long.valueOf(path);
            } catch (RuntimeException e) {
            }
        }

        GeoTownRoute.getRoute(routeId, ROUTE_REQ_ID);

        updateRouteUI();
    }

    private void loadWaypoints() {
        if (waypointsLoaded) return;
        GetRouteWaypointsRequest getRouteWaypointsRequest = new GetRouteWaypointsRequest();
        getRouteWaypointsRequest.execute(routeId);
        waypointsLoaded = true;
    }

    private void updateRouteUI() {
        if (mRoute == null || !created) {
            return;
        }

        FragmentManager fm = this.getFragmentManager();
        mMapFragment = (MapFragment) fm.findFragmentById(R.id.map);

        routeName.setText(Html.fromHtml("<b>" + mRoute.name + "</b>"));
        routeOwner.setText(Html.fromHtml("<i>by " + mRoute.owner + "</i>"));


        if (GeotownApplication.getPreferences().getLong("current_route", 0L) == mRoute.id) {
            playRoute.setText(R.string.currently_playing);
            playRoute.setEnabled(false);
        }


        if (mMapFragment == null) return;
        mMapFragment.getMap().setMyLocationEnabled(false);
        mMapFragment.getMap().setTrafficEnabled(false);
        mMapFragment.getMap().setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mRoute.latitude, mRoute.longitude), 14.0f));
        mMapFragment.getMap().addMarker(new MarkerOptions().position(new LatLng(mRoute.latitude, mRoute.longitude)).title(mRoute.name).snippet(mRoute.owner));
        mMapFragment.getMap().getUiSettings().setAllGesturesEnabled(false);
    }


    @Subscribe
    public void onGeoTownRouteRetrieved(GeoTownRouteRetrievedEvent event) {
        if (routeRetrieved) return;
        if (event.id != ROUTE_REQ_ID) return;
        mRoute = event.route;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateRouteUI();

            }
        });
        loadWaypoints();
        routeRetrieved = true;
    }

    @Override
    protected void onPause() {
        finish();
        super.onPause();
    }

    @Subscribe
    public void onRouteWaypointsReceived(RouteWaypointsReceivedEvent event) {
        int waypointCount = 0;
        if (event.waypoints != null) {
            waypointCount = event.waypoints.size();
        }

        routeWaypoints.setText(waypointCount + " " + getResources().getString(R.string.waypoints));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.route_detail, menu);

        MenuItem item = menu.findItem(R.id.share_route);

        mShareActionProvider = (ShareActionProvider) item.getActionProvider();

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        Intent routeIntent = new Intent();
        routeIntent.setAction("de.happycarl.geotown.app.ROUTE_ID");
        routeIntent.putExtra("routeID", mRoute.id);


        String routeShare = "http://geotown.de/" + mRoute.id;
        String shareTextRaw = getResources().getString(R.string.share_text);
        String shareText = String.format(shareTextRaw, routeShare, "(Not yet in store)");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.setType("text/plain");

        mShareActionProvider.setShareIntent(shareIntent);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }


    @OnClick(R.id.play_route)
    public void playCurrentRoute() {
        SharedPreferences pref = GeotownApplication.getPreferences();
        final SharedPreferences.Editor editor = GeotownApplication.getPreferences().edit();
        if (pref.getLong("current_route", 0L) != 0L) { //User is currently playing a different route

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            editor.putLong("current_route", 0L);
                            editor.apply();
                            //deleted current Route, calling again
                            //Not nice, but simple
                            playCurrentRoute();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            //Do nothing, user cancelled
                            //TODO: Maybe go back to overview screen???
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setMessage(R.string.cancel_current_route)
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setNegativeButton(android.R.string.no, dialogClickListener).show();

        } else { //No current Route


            editor.putLong("current_route", mRoute.id);
            editor.apply();
            finish(); //Back to Overview screen
        }
    }
}
