package de.happycarl.geotown.app.gui;

import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.cardsui.Card;
import com.afollestad.cardsui.CardBase;
import com.afollestad.cardsui.CardHeader;
import com.afollestad.cardsui.CardListView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.cketti.library.changelog.ChangeLog;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.requests.AllMyRoutesRequest;
import de.happycarl.geotown.app.api.requests.NearRoutesRequest;
import de.happycarl.geotown.app.events.db.GeoTownRouteDeletedEvent;
import de.happycarl.geotown.app.events.db.GeoTownRouteRetrievedEvent;
import de.happycarl.geotown.app.events.net.MyRoutesDataReceivedEvent;
import de.happycarl.geotown.app.events.net.NearRoutesDataReceivedEvent;
import de.happycarl.geotown.app.gui.data.OverviewCardsAdapter;
import de.happycarl.geotown.app.gui.views.RouteCard;

public class OverviewActivity extends SystemBarTintActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, CardListView.CardClickListener, SwipeRefreshLayout.OnRefreshListener {

    //================================================================================
    // Constants
    //================================================================================

    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 421;
    private static final int GET_ROUTE_BY_NAME_DETAIL_REQUEST = 425498458;
    private static final int GET_FOREIGN_ROUTES_REQUEST = 6875358;
    private static final int DEFAULT_NEAR_ROUTES_SEARCH_RADIUS = 1000000; // in m (afaik :) )
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;

    //================================================================================
    // Properties
    //================================================================================

    @InjectView(R.id.route_view)
    CardListView cardListView;

    @InjectView(R.id.overview_card_ptr_layout)
    SwipeRefreshLayout cardUILayout;
    private OverviewCardsAdapter adapter;
    private LocationClient locationClient;
    private LocationRequest locationRequest;

    private boolean locationUpdateReceived = false;

    //================================================================================
    // Activity Lifecycle
    //================================================================================
    private CardHeader.ActionListener nearRoutesActionListener = new CardHeader.ActionListener() {

        @Override
        public void onHeaderActionClick(CardHeader cardHeader) {
            // TODO: Show "NearRoutesActivity" here.
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);

        cardUILayout.setOnRefreshListener(this);
        cardUILayout.setColorScheme(R.color.primary_color, android.R.color.holo_blue_light, R.color.primary_color, android.R.color.holo_blue_light);

        adapter = new OverviewCardsAdapter(this);
        cardListView.setAdapter(adapter);

        locationClient = new LocationClient(this, this, this);

        cardListView.setOnCardClickListener(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(
                LocationRequest.PRIORITY_LOW_POWER);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        refreshRoutes();

        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            cl.getLogDialog().show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.refreshRoutes();
        this.adapter.startRefreshSavedRoutes();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GeotownApplication.getEventBus().unregister(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationClient.connect();
        this.refreshRoutes();
    }

    @Override
    protected void onStop() {
        if (locationClient.isConnected()) {
            locationClient.removeLocationUpdates(this);
        }

        // Disconnecting the client invalidates it.
        locationClient.disconnect();
        super.onStop();
    }
    //================================================================================
    // UI
    //================================================================================


    @Override
    public void onCardClick(int index, CardBase item, View view) {
        Card c = (Card) item;
        RouteCard rc = (RouteCard) c;
        Log.d("Clicked", rc.getRouteID() + "");

        Intent intent = new Intent(this, RouteDetailActivity.class);
        intent.putExtra("routeID", rc.getRouteID());
        startActivity(intent);
        //GeoTownRoute.getRoute(item.getTitle().toString(), GET_ROUTE_BY_NAME_DETAIL_REQUEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.overview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            //case R.id.action_settings:
            //    return true;
            case R.id.action_close:
                finish();
                return true;
            case R.id.action_about:

                break;
            case R.id.action_achievemnts:
                startActivityForResult(Games.Achievements.getAchievementsIntent(GeotownApplication.getGameHelper().getApiClient()), 42);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        this.refreshRoutes();
    }

    //================================================================================
    // Network & Database
    //================================================================================

    private void refreshRoutes() {
        loadMyRoutes();
        loadCurrentRoute();
        locationUpdateReceived = false;
    }

    private void loadCurrentRoute() {
        this.adapter.startRefreshCurrentRoute();
    }

    private void loadMyRoutes() {
        GeotownApplication.getJobManager().addJob(new AllMyRoutesRequest());
    }

    private void loadNearRoutes(Location currentLocation) {
        Log.d("OverviewActivity", locationClient.isConnected() + "");
        GeotownApplication.getJobManager().addJob(new NearRoutesRequest(currentLocation.getLatitude(), currentLocation.getLongitude(), DEFAULT_NEAR_ROUTES_SEARCH_RADIUS));
    }

    @Subscribe
    public void onRouteDeleted(GeoTownRouteDeletedEvent event) {
        adapter.startRefreshSavedRoutes();
    }

    @Subscribe
    public void onGeoTownRouteRetrieved(GeoTownRouteRetrievedEvent event) {
        if (event.id == GET_ROUTE_BY_NAME_DETAIL_REQUEST) {
            Intent intent = new Intent(this, RouteDetailActivity.class);
            intent.putExtra("routeID", event.route.id);
            startActivity(intent);
        }
    }

    @Subscribe
    public void onNearRoutesDataReceived(NearRoutesDataReceivedEvent event) {
        adapter.startRefreshNearRoutes();
        this.cardUILayout.setRefreshing(false);
    }

    @Subscribe
    public void onMyRoutesDataReceived(MyRoutesDataReceivedEvent event) {
        Log.d("Update", "Received my Routes.");
        adapter.startRefreshMyRoutes();
    }

    //================================================================================
    // GeoLocation Services
    //================================================================================


    @Override
    public void onConnected(Bundle bundle) {
        locationClient.requestLocationUpdates(locationRequest, this);
        Log.d("OverviewActivity", "Location Client connected!");
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (!locationUpdateReceived)
            loadNearRoutes(location);

        this.locationUpdateReceived = true;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.e("OverviewActivity", "Couldn't connect to location client: " + connectionResult.getErrorCode());
        }
    }

}
