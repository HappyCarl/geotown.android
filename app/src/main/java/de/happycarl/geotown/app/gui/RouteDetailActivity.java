package de.happycarl.geotown.app.gui;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;

import com.afollestad.cardsui.CardAdapter;
import com.afollestad.cardsui.CardListView;
import com.appspot.drive_log.geotown.model.Route;
import com.appspot.drive_log.geotown.model.Waypoint;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.requests.GetRouteWaypointsRequest;
import de.happycarl.geotown.app.api.requests.RouteRequest;
import de.happycarl.geotown.app.events.db.GeoTownRouteRetrievedEvent;
import de.happycarl.geotown.app.events.db.GeoTownWaypointsAddedEvent;
import de.happycarl.geotown.app.events.net.RouteDataReceivedEvent;
import de.happycarl.geotown.app.events.net.RouteWaypointsReceivedEvent;
import de.happycarl.geotown.app.gui.views.LoadingCard;
import de.happycarl.geotown.app.gui.views.RouteActionsCard;
import de.happycarl.geotown.app.gui.views.RouteDetailCard;
import de.happycarl.geotown.app.gui.views.RouteDetailCardAdapter;
import de.happycarl.geotown.app.models.GeoTownRoute;
import de.happycarl.geotown.app.models.GeoTownWaypoint;

public class RouteDetailActivity extends SystemBarTintActivity implements RouteActionsCard.RouteActionsCardListener {

    public static final int REQUEST_ROUTE_ID = 876354;

    //================================================================================
    // Properties
    //================================================================================

    @InjectView(R.id.route_detail_card_list)
    CardListView cardsList;

    private MapFragment mMapFragment;
    private ShareActionProvider mShareActionProvider;
    private CardAdapter mCardAdapter;
    private RouteDetailCard mRouteDetailCard;
    private RouteActionsCard mRouteActionsCard;

    private long routeId = -1;
    private Route mRoute;
    private List<Waypoint> mWaypoints = new ArrayList<>();

    private NfcAdapter mNfcAdapter;
    private NdefMessage mNdefMessage;


    //================================================================================
    // Activity Lifecycle
    //================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detail);

        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);


        mCardAdapter = new RouteDetailCardAdapter(this, R.color.primary_color, mRoute);
        cardsList.setAdapter(mCardAdapter);

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

        Log.d("RouteOverview", "" + routeId);
        if (routeId == -1L) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setCancelable(false);
            b.setTitle(R.string.not_found);
            b.setMessage(R.string.not_found_detail);
            b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            b.show();
        } else {
            loadRoute();

            updateRouteUI();

            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        }


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

    @Override
    protected void onResume() {
        Log.d("OnResume", "resumed");


        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            NdefMessage[] msgs;
            Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }

                Log.d("NFC", msgs[0].getRecords()[0].toString());
            }
        }


        super.onResume();
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
        updateCardsList();
        if (mRoute == null) {
            return;
        }

        if (getActionBar() != null) {
            getActionBar().setTitle(mRoute.getName());
        }

        FragmentManager fm = this.getFragmentManager();
        mMapFragment = (MapFragment) fm.findFragmentById(R.id.map);


        mMapFragment.getMap().setMyLocationEnabled(false);
        mMapFragment.getMap().setTrafficEnabled(false);
        mMapFragment.getMap().setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mRoute.getLatitude(), mRoute.getLongitude()), 14.0f));
        mMapFragment.getMap().addMarker(new MarkerOptions().position(new LatLng(mRoute.getLatitude(), mRoute.getLongitude())).title(mRoute.getName()).snippet(mRoute.getOwner().getUsername()));
        mMapFragment.getMap().getUiSettings().setAllGesturesEnabled(false);

        updateShareIntent();
        updateAndroidBeamPayload();
    }

    private void updateCardsList() {
        mCardAdapter.clear();

        if (mRoute != null) {
            mRouteDetailCard = new RouteDetailCard(this, mCardAdapter, mRoute);
            mRouteActionsCard = new RouteActionsCard(this, this, mRoute);
        } else {
            mCardAdapter.add(new LoadingCard());
        }

    }


    private void updateShareIntent() {
        if (mShareActionProvider == null) return;
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        Intent routeIntent = new Intent();
        routeIntent.setAction("de.happycarl.geotown.app.ROUTE_ID");
        routeIntent.putExtra("routeID", routeId);


        String routeShare = AppConstants.SHARE_DOMAIN_NAME + AppConstants.SHARE_PATH_PREFIX + routeId;
        String shareTextRaw = getResources().getString(R.string.share_text);
        String shareText = String.format(shareTextRaw, routeShare, "(Not yet in store)");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.setType("text/plain");

        mShareActionProvider.setShareIntent(shareIntent);
    }

    private void updateAndroidBeamPayload() {
        if (mNfcAdapter != null) {
            NdefRecord[] records = new NdefRecord[]{NdefRecord.createUri(AppConstants.SHARE_DOMAIN_NAME + AppConstants.SHARE_PATH_PREFIX + mRoute.getId()), NdefRecord.createApplicationRecord("de.happycarl.geotown.app")};

            mNdefMessage = new NdefMessage(records);

            mNfcAdapter.setNdefPushMessage(mNdefMessage, this);
        }
    }


    @Override
    public void onCheckBoxClicked(boolean status) {
        if (status) {
            GeoTownRoute.update(mRoute, true);
        } else {
            GeoTownRoute.deleteRoute(mRoute.getId());
        }
    }

    @Override
    public void onPlayButtonClicked() {
        SharedPreferences pref = GeotownApplication.getPreferences();
        final SharedPreferences.Editor editor = GeotownApplication.getPreferences().edit();
        if (pref.getLong(AppConstants.PREF_CURRENT_ROUTE, 0L) != 0L) { //User is currently playing a different route

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            editor.putLong(AppConstants.PREF_CURRENT_ROUTE, 0L);
                            editor.apply();
                            //deleted current Route, calling again
                            //Not nice, but simple #olaf
                            onPlayButtonClicked();
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


            editor.putLong(AppConstants.PREF_CURRENT_ROUTE, mRoute.getId());
            editor.apply();
            GeoTownRoute.update(mRoute, true);
            GeoTownWaypoint.addWaypoints(mWaypoints);

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
        GeoTownRoute.getRoute(mRoute.getId(), REQUEST_ROUTE_ID);
        loadWaypoints();
    }

    @Subscribe
    public void onWaypointsAdded(GeoTownWaypointsAddedEvent event) {
        if (event.success) {
            finish();
        }
    }


    @Subscribe
    public void onRouteWaypointsReceived(RouteWaypointsReceivedEvent event) {
        Log.d("Routes", "Waypoints Event received" + event);
        int waypointCount = 0;
        if (event.waypoints != null) {
            waypointCount = event.waypoints.size();
            mRouteDetailCard.setWaypointAmount(waypointCount);

            mWaypoints = event.waypoints;
        }

        //routeWaypoints.setText(waypointCount + " " + getResources().getString(R.string.waypoints));
    }

    @Subscribe
    public void onRouteReceived(GeoTownRouteRetrievedEvent event) {
        if (event.id != REQUEST_ROUTE_ID) return;
        if (event.route != null && event.route.mine == false) {
            //star.setChecked(true);
            mRouteActionsCard.setSaved(true);
        }
    }

}
