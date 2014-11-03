package de.happycarl.geotown.app.gui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.cardsui.Card;
import com.afollestad.cardsui.CardBase;
import com.afollestad.cardsui.CardListView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

import java.util.Arrays;

import de.cketti.library.changelog.ChangeLog;
import de.happycarl.geotown.app.AppConstants;
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
import de.happycarl.geotown.app.licenses.AndroidAnnotationLicense;
import de.happycarl.geotown.app.licenses.GeoTownLicense;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

@EActivity()
@OptionsMenu(R.menu.overview)
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

    @ViewById(R.id.route_view)
    CardListView cardListView;

    @ViewById(R.id.overview_card_ptr_layout)
    SwipeRefreshLayout cardUILayout;
    private OverviewCardsAdapter adapter;
    private LocationClient locationClient;
    private LocationRequest locationRequest;

    private boolean locationUpdateReceived = false;

    //================================================================================
    // Activity Lifecycle
    //================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_overview);

        GeotownApplication.getEventBus().register(this);
    }

    @AfterViews
    protected void afterViews() {
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

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);

        IntentResult scanResult = IntentIntegrator.parseActivityResult(request, response, data);
        if (scanResult != null) {
            String contents = scanResult.getContents();
            if (contents == null || contents.isEmpty()) return;

            String[] splitResult = contents.split(":");
            Log.d("QR-Scan", Arrays.toString(splitResult));
            if (splitResult.length == 3) {

                if (!splitResult[0].equals(AppConstants.QR_CODE_PREFIX)) {
                    Crouton.makeText(this, R.string.message_overview_invalid_qr, Style.ALERT).show();
                    Log.d("QR-Scan", "First part did not match prefix");
                    return;
                }

                try {

                    long routeId = Long.parseLong(splitResult[1]);
                    long waypointId = Long.parseLong(splitResult[2]);

                    GeotownApplication.getPreferences().edit()
                            .putLong(AppConstants.PREF_CURRENT_ROUTE, routeId)
                            .putLong(AppConstants.PREF_CURRENT_WAYPOINT, waypointId)
                            .apply();
                    startRouteDetailActivity(routeId);

                } catch (NumberFormatException e) {
                    Crouton.makeText(this, R.string.message_overview_invalid_qr, Style.ALERT).show();
                    Log.d("QR-Scan", "number parsing failed");
                }


            } else {
                Crouton.makeText(this, R.string.message_overview_invalid_qr, Style.ALERT).show();
                Log.d("QR-Scan", "does not consist of 3 parts");
            }
        }


    }
    //================================================================================
    // UI
    //================================================================================


    @Override
    public void onCardClick(int index, CardBase item, View view) {
        Card c = (Card) item;
        RouteCard rc = (RouteCard) c;
        Log.d("Clicked", rc.getRouteID() + "");

        startRouteDetailActivity(rc.getRouteID());
        //GeoTownRoute.getRoute(item.getTitle().toString(), GET_ROUTE_BY_NAME_DETAIL_REQUEST);
    }

    private void startRouteDetailActivity(long routeID) {
        RouteDetailActivity_.intent(this).extra("routeID", routeID).start();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_upload_track_data).setChecked(GeotownApplication.getPreferences().getBoolean(AppConstants.PREF_SUBMIT_TRACK_DATA, false));
        return true;
    }

    @OptionsItem(R.id.action_close)
    void closeSelected() {
        finish();
    }

    @OptionsItem(R.id.action_achievements)
    void achievementsSelected() {
        if (mGameHelper.isSignedIn()) {
            startActivityForResult(Games.Achievements.getAchievementsIntent(mGameHelper.getApiClient()), 42);
        } else {
            Crouton.makeText(this, R.string.gplus_first_sign_in, Style.INFO).show();
        }
    }

    @OptionsItem(R.id.action_leaderboard)
    void leaderboardSelected() {
        if (mGameHelper.isSignedIn()) {
            startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mGameHelper.getApiClient(),
                    getString(R.string.leaderboard_routes)), 43);
        } else {
            Crouton.makeText(this, R.string.gplus_first_sign_in, Style.INFO).show();
        }
    }

    @OptionsItem(R.id.action_glpus_sign_out)
    void gplusSignoutSelected() {
        if (mGameHelper.isSignedIn())
            mGameHelper.signOut();
    }

    @OptionsItem(R.id.action_scan_qr_route)
    void scanQRRouteSelected() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    @OptionsItem(R.id.action_upload_track_data)
    void uploadTrackSelected(MenuItem item) {
        Log.d("OverviewActivity", "Clicked Upload GPS");
        if (!item.isChecked()) {
            final MenuItem finItem = item;
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            GeotownApplication.getPreferences().edit().putBoolean(AppConstants.PREF_SUBMIT_TRACK_DATA, true).apply();
                            finItem.setChecked(true);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            GeotownApplication.getPreferences().edit().putBoolean(AppConstants.PREF_SUBMIT_TRACK_DATA, false).apply();
                            finItem.setChecked(false);
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setMessage(R.string.message_overview_submit_track_info)
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setNegativeButton(android.R.string.no, dialogClickListener).show();
        } else {
            GeotownApplication.getPreferences().edit().putBoolean(AppConstants.PREF_SUBMIT_TRACK_DATA, false).apply();
            item.setChecked(false);
        }
    }

    @OptionsItem(R.id.action_credits)
    void showOpenSourceLicenses() {
        final Notices notices = new Notices();

        notices.addNotice(new Notice("GeoTown",
                "geotown.de",
                "Coypright 2014 HappyCarl (Jan-Henrik Bruhn, Ole Wehrmeyer)",
                new GeoTownLicense()));

        notices.addNotice(new Notice("AndroidAnnotations",
                "http://androidannotations.org/",
                "Copyright 2012-2014 eBusiness Information",
                new AndroidAnnotationLicense()));

        notices.addNotice(new Notice("SystemBarTint",
                "https://github.com/jgilfelt/SystemBarTint",
                "Copyright jgilfelt",
                new ApacheSoftwareLicense20()));

        notices.addNotice(new Notice("EventBus",
                "https://github.com/greenrobot/EventBus",
                "Copyright (C) 2012-2014 Markus Junginger, greenrobot (http://greenrobot.de)",
                new ApacheSoftwareLicense20()));

        notices.addNotice(new Notice("Picasso",
                "http://square.github.io/picasso/",
                "Copyright 2013 Square, Inc.",
                new ApacheSoftwareLicense20()));

        notices.addNotice(new Notice("OkHttp",
                "http://square.github.io/okhttp/",
                "Copyright 2014 Square, Inc.",
                new ApacheSoftwareLicense20()));

        notices.addNotice(new Notice("Android-Priority-Jobqueue",
                "https://github.com/path/android-priority-jobqueue",
                "Copyright (c) 2013 Path, Inc.",
                new MITLicense()));

        notices.addNotice(new Notice("ckChangeLog",
                "https://github.com/cketti/ckChangeLog",
                "Copyright cketti",
                new ApacheSoftwareLicense20()));

        notices.addNotice(new Notice("Crouton",
                "https://github.com/keyboardsurfer/Crouton",
                "Copyright 2012 - 2014 Benjamin Weiss",
                new ApacheSoftwareLicense20()));


        new LicensesDialog.Builder(this).setTitle("Open Source Licenses").setNotices(notices).setIncludeOwnLicense(true).build().show();

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

    public void onEvent(GeoTownRouteDeletedEvent event) {
        adapter.startRefreshSavedRoutes();
    }

    public void onEvent(GeoTownRouteRetrievedEvent event) {
        if (event.id == GET_ROUTE_BY_NAME_DETAIL_REQUEST) {
            Intent intent = new Intent(this, RouteDetailActivity.class);
            intent.putExtra("routeID", event.route.id);
            startActivity(intent);
        }
    }

    public void onEvent(NearRoutesDataReceivedEvent event) {
        adapter.startRefreshNearRoutes();
        this.cardUILayout.setRefreshing(false);
    }

    public void onEvent(MyRoutesDataReceivedEvent event) {
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
