package de.happycarl.geotown.app.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.requests.GetRouteWaypointsRequest;
import de.happycarl.geotown.app.events.RouteWaypointsReceivedEvent;
import de.happycarl.geotown.app.models.GeoTownRoute;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

public class RouteDetail extends SystemBarTintActivity {

    @InjectView(R.id.detail_route_name)
    TextView routeName;

    @InjectView(R.id.detail_route_owner)
    TextView routeOwner;

    @InjectView(R.id.detail_route_waypoints)
    TextView routeWaypoints;

    @InjectView(R.id.play_route)
    Button playRoute;

    MapFragment map;

    GeoTownRoute route;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detail);

        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);

        route = GeoTownRoute.getRoute(getIntent().getLongExtra("routeID",0L));

        routeName.setText(Html.fromHtml("<b>" + route.name + "</b>"));
        routeOwner.setText(Html.fromHtml("<i>by "+route.owner + "</i>"));

        GetRouteWaypointsRequest getRouteWaypointsRequest = new GetRouteWaypointsRequest();
        getRouteWaypointsRequest.execute(route.id);

        if(GeotownApplication.getPreferences().getLong("current_route",0L) == route.id) {
            playRoute.setText(R.string.currently_playing);
            playRoute.setEnabled(false);
        }

        FragmentManager fm = this.getFragmentManager();
        map = (MapFragment)fm.findFragmentById(R.id.map);
        map.getMap().setMyLocationEnabled(false);
        map.getMap().setTrafficEnabled(false);
        map.getMap().setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        map.getMap().moveCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(route.latitude,route.longitude) , 14.0f) );
        map.getMap().addMarker(new MarkerOptions().position(new LatLng(route.latitude,route.longitude)).title(route.name).snippet(route.owner));
        map.getMap().getUiSettings().setAllGesturesEnabled(false);


    }

    @Subscribe
    public void onRouteWaypointsReceived(RouteWaypointsReceivedEvent event) {
        Log.d("RouteDetail","Got waypoints");
        int waypointCount = 0;
        if(event.waypoints != null) {
            waypointCount = event.waypoints.size();
        }

        routeWaypoints.setText(waypointCount + " " + getResources().getString(R.string.waypoints));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.route_detail, menu);
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
        if(pref.getLong("current_route", 0L) != 0L) { //User is currently playing a different route

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            editor.putLong("current_route",0L);
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

            editor.putLong("current_route",route.id);
            editor.apply();
            finish(); //Back to Overview screen
        }
    }
}
