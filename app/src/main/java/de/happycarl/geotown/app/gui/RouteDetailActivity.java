package de.happycarl.geotown.app.gui;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.appspot.drive_log.geotown.model.Route;
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
import de.happycarl.geotown.app.api.requests.RouteRequest;
import de.happycarl.geotown.app.events.net.RouteDataReceivedEvent;
import de.happycarl.geotown.app.events.net.RouteWaypointsReceivedEvent;

public class RouteDetailActivity extends SystemBarTintActivity {

    //================================================================================
    // Properties
    //================================================================================


    @InjectView(R.id.detail_route_name)
    private TextView routeName;

    @InjectView(R.id.detail_route_owner)
    private TextView routeOwner;

    @InjectView(R.id.detail_route_waypoints)
    private TextView routeWaypoints;

    @InjectView(R.id.play_route)
    private Button playRoute;

    private MapFragment mMapFragment;
    private ShareActionProvider mShareActionProvider;

    private long routeId = -1;
    private Route mRoute;


    //================================================================================
    // Activity Lifecycle
    //================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detail);

        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);

        String path = "";
        if (getIntent().getData() != null && getIntent().getData().getPath() != null)
            path = getIntent().getData().getPath().replaceAll("[^\\d]", "");

        routeId = getIntent().getLongExtra("routeID", -1L);

        if (routeId == -1 && !path.isEmpty()) {
            try {
                routeId = Long.valueOf(path);
            } catch (RuntimeException e) {
            }
        } else {
            if (getActionBar() != null) {
                getActionBar().setHomeButtonEnabled(true);
                getActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        loadRoute();

        updateRouteUI();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GeotownApplication.getEventBus().unregister(this);
    }


    @Override
    protected void onPause() {
        finish();
        super.onPause();
    }

    //================================================================================
    // UI
    //================================================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.route_detail, menu);

        MenuItem item = menu.findItem(R.id.share_route);

        mShareActionProvider = (ShareActionProvider) item.getActionProvider();

        updateShareIntent();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateRouteUI() {
        if (mRoute == null) {
            return;
        }

        if (getActionBar() != null) {
            getActionBar().setTitle(mRoute.getName());
        }

        FragmentManager fm = this.getFragmentManager();
        mMapFragment = (MapFragment) fm.findFragmentById(R.id.map);

        routeName.setText(Html.fromHtml("<b>" + mRoute.getName() + "</b>"));
        routeOwner.setText(Html.fromHtml("<i>by " + mRoute.getOwner().getUsername() + "</i>"));


        if (GeotownApplication.getPreferences().getLong("current_route", 0L) == mRoute.getId()) {
            playRoute.setText(R.string.currently_playing);
            playRoute.setEnabled(false);
        }


        mMapFragment.getMap().setMyLocationEnabled(false);
        mMapFragment.getMap().setTrafficEnabled(false);
        mMapFragment.getMap().setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mRoute.getLatitude(), mRoute.getLongitude()), 14.0f));
        mMapFragment.getMap().addMarker(new MarkerOptions().position(new LatLng(mRoute.getLatitude(), mRoute.getLongitude())).title(mRoute.getName()).snippet(mRoute.getOwner().getUsername()));
        mMapFragment.getMap().getUiSettings().setAllGesturesEnabled(false);

        updateShareIntent();
    }


    private void updateShareIntent() {
        if (mShareActionProvider == null) return;
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        Intent routeIntent = new Intent();
        routeIntent.setAction("de.happycarl.geotown.app.ROUTE_ID");
        routeIntent.putExtra("routeID", routeId);


        String routeShare = "http://geotown.de/" + routeId;
        String shareTextRaw = getResources().getString(R.string.share_text);
        String shareText = String.format(shareTextRaw, routeShare, "(Not yet in store)");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.setType("text/plain");

        mShareActionProvider.setShareIntent(shareIntent);
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


            editor.putLong("current_route", mRoute.getId());
            editor.apply();
            finish(); //Back to Overview screen
        }
    }


    //================================================================================
    // Network
    //================================================================================

    private void loadRoute() {
        new RouteRequest().execute(routeId);
    }

    private void loadWaypoints() {
        GetRouteWaypointsRequest getRouteWaypointsRequest = new GetRouteWaypointsRequest();
        getRouteWaypointsRequest.execute(routeId);
    }

    @Subscribe
    public void onRouteReceived(RouteDataReceivedEvent event) {
        mRoute = event.route;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateRouteUI();

            }
        });
        loadWaypoints();
    }


    @Subscribe
    public void onRouteWaypointsReceived(RouteWaypointsReceivedEvent event) {
        Log.d("Routes", "Waypoints Event received" + event);
        int waypointCount = 0;
        if (event.waypoints != null) {
            waypointCount = event.waypoints.size();
        }

        routeWaypoints.setText(waypointCount + " " + getResources().getString(R.string.waypoints));
    }

}
