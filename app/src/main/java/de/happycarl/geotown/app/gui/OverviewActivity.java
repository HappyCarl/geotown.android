package de.happycarl.geotown.app.gui;

import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.afollestad.cardsui.CardAdapter;
import com.afollestad.cardsui.CardBase;
import com.afollestad.cardsui.CardCenteredHeader;
import com.afollestad.cardsui.CardHeader;
import com.afollestad.cardsui.CardListView;
import com.appspot.drive_log.geotown.model.Route;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.GoogleUtils;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.requests.AllMyRoutesRequest;
import de.happycarl.geotown.app.api.requests.NearRoutesRequest;
import de.happycarl.geotown.app.events.db.GeoTownRouteRetrievedEvent;
import de.happycarl.geotown.app.events.net.MyRoutesDataReceivedEvent;
import de.happycarl.geotown.app.events.net.NearRoutesDataReceivedEvent;
import de.happycarl.geotown.app.gui.views.RouteCard;
import de.happycarl.geotown.app.models.GeoTownRoute;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class OverviewActivity extends SystemBarTintActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, OnRefreshListener, CardListView.CardClickListener {

    //================================================================================
    // Constants
    //================================================================================

    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 421;
    private static final int GET_ROUTE_BY_NAME_DETAIL_REQUEST = 425498458;

    private static final int DEFAULT_NEAR_ROUTES_SEARCH_RADIUS = 1000000; // in m (afaik :) )

    private static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
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

    @InjectView(R.id.overview_loading_layout)
    LinearLayout loadingLayout;

    @InjectView(R.id.overview_card_ptr_layout)
    PullToRefreshLayout cardUILayout;

    private CardAdapter adapter;

    private List<Route> myRoutes = new ArrayList<Route>();
    private List<Route> nearRoutes = new ArrayList<Route>();
    boolean loadingMyRoutes = false;
    boolean loadingNearRoutes = true;

    private LocationClient locationClient;
    private LocationRequest locationRequest;

    private boolean locationUpdateReceived = false;

    //================================================================================
    // Activity Lifecycle
    //================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);

        ActionBarPullToRefresh.from(this)
                .allChildrenArePullable()
                .listener(this)
                .setup(cardUILayout);

        adapter = new CardAdapter(this, R.color.primary_color);
        cardListView.setAdapter(adapter);

        locationClient = new LocationClient(this, this, this);

        cardListView.setOnCardClickListener(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(
                LocationRequest.PRIORITY_LOW_POWER);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        refreshRoutes();
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
        GeoTownRoute.getRoute(item.getTitle().toString(), GET_ROUTE_BY_NAME_DETAIL_REQUEST);
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

        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("rawtypes unchecked")
    private void updateCardsUI() {
        adapter.clear();

        // Near ROutes
        CardHeader header = new CardHeader(getResources().getString(R.string.near_routes));
        header.setClickable(true);
        header.setAction(getResources().getString(R.string.see_more), nearRoutesActionListener);
        adapter.add(header);

        if (nearRoutes == null || nearRoutes.size() == 0) {
            CardCenteredHeader empty = null;
            if (locationUpdateReceived)
                empty = new CardCenteredHeader(getResources().getString(R.string.no_near_routes));
            else
                empty = new CardCenteredHeader(getString(R.string.near_routes_no_location));
            adapter.add(empty);
        }

        if (nearRoutes != null) {
            int i = 0; // Only show 3 near routes.
            for (Route r : nearRoutes) {
                if (i >= 3) break;
                RouteCard c = new RouteCard(this, adapter, r);
                Picasso.with(this).load(GoogleUtils.getStaticMapUrl(r.getLatitude(), r.getLongitude(), 8, 128)).placeholder(R.drawable.ic_launcher).into(c);
                GeoTownRoute.update(r, true);
                i++;
            }
        }

        // My Routes
        CardHeader header2 = new CardHeader(getResources().getString(R.string.my_routes));
        adapter.add(header2);

        if (myRoutes == null || myRoutes.size() == 0) {
            CardCenteredHeader empty = new CardCenteredHeader(getResources().getString(R.string.no_routes));
            adapter.add(empty);
        }

        if (myRoutes != null) {
            for (Route r : myRoutes) {
                RouteCard c = new RouteCard(this, adapter, r);
                Picasso.with(this).load(GoogleUtils.getStaticMapUrl(r.getLatitude(), r.getLongitude(), 8, 128)).placeholder(R.drawable.ic_launcher).into(c);

                GeoTownRoute.update(r, true);
            }
        }
        loadingLayout.setVisibility(View.GONE);
        cardUILayout.setVisibility(View.VISIBLE);

        this.cardUILayout.setRefreshComplete();

    }

    @Override
    public void onRefreshStarted(View view) {
        this.refreshRoutes();
    }

    private void refreshRoutes() {
        loadingLayout.setVisibility(View.VISIBLE);
        cardUILayout.setVisibility(View.GONE);

        loadMyRoutes();
        locationUpdateReceived = false;
    }

    //================================================================================
    // Network
    //================================================================================

    private void loadMyRoutes() {
        AllMyRoutesRequest routesRequest = new AllMyRoutesRequest();
        routesRequest.execute((Void) null);
        loadingMyRoutes = true;
    }

    private void loadNearRoutes(Location currentLocation) {
        Log.d("OverviewActivity", locationClient.isConnected() + "");
        NearRoutesRequest nearRoutesRequest = new NearRoutesRequest();
        nearRoutesRequest.execute(new NearRoutesRequest.NearRoutesParams(currentLocation.getLatitude(), currentLocation.getLongitude(), DEFAULT_NEAR_ROUTES_SEARCH_RADIUS));
        loadingNearRoutes = true;
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
        this.nearRoutes = event.routes;
        loadingNearRoutes = false;

        if (!loadingMyRoutes)
            updateCardsUI();
    }

    @Subscribe
    public void onMyRoutesDataReceived(MyRoutesDataReceivedEvent event) {
        myRoutes = event.routes;
        loadingMyRoutes = false;

        if (!loadingNearRoutes)
            updateCardsUI();
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
        updateCardsUI();

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

    private CardHeader.ActionListener nearRoutesActionListener = new CardHeader.ActionListener() {

        @Override
        public void onHeaderActionClick(CardHeader cardHeader) {
            // TODO: Show "NearRoutesActivity" here.
        }
    };

}
