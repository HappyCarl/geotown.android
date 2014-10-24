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
import android.view.Window;
import android.widget.ShareActionProvider;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;
import com.afollestad.cardsui.CardListView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.requests.RouteRequest;
import de.happycarl.geotown.app.events.net.RouteDataReceivedEvent;
import de.happycarl.geotown.app.gui.views.LoadingCard;
import de.happycarl.geotown.app.gui.views.RouteActionsCard;
import de.happycarl.geotown.app.gui.views.RouteDetailCard;
import de.happycarl.geotown.app.gui.views.RouteDetailCardAdapter;
import de.happycarl.geotown.app.models.GeoTownRoute;
import de.happycarl.geotown.app.models.GeoTownWaypoint;

@EActivity(R.layout.activity_route_detail)
public class RouteDetailActivity extends SystemBarTintActivity implements RouteActionsCard.RouteActionsCardListener {
    //================================================================================
    // Properties
    //================================================================================

    @ViewById(R.id.route_detail_card_list)
    CardListView cardsList;

    private ShareActionProvider mShareActionProvider;
    private RouteDetailCardAdapter mCardAdapter;

    private long routeId = -1;
    private GeoTownRoute mRoute;

    private NfcAdapter mNfcAdapter;


    //================================================================================
    // Activity Lifecycle
    //================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);


        GeotownApplication.getEventBus().register(this);
    }

    @AfterViews
    protected void afterViews() {
        mCardAdapter = new RouteDetailCardAdapter(this, R.color.primary_color);
        cardsList.setAdapter(mCardAdapter);

        initRouteID();

    }

    private void initRouteID() {
        String path = "";
        if (getIntent().getData() != null && getIntent().getData().getPath() != null)
            path = getIntent().getData().getPath().replaceAll("[^\\d]", "");

        routeId = getIntent().getLongExtra("routeID", -1L);

        if (routeId == -1 && !path.isEmpty()) {
            try {
                routeId = Long.valueOf(path);
            } catch (RuntimeException e) {
                e.printStackTrace();
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
            b.setTitle(R.string.error_routedetail_not_found);
            b.setMessage(R.string.error_routedetail_not_found_detail);
            b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            b.show();
        } else {
            updateRouteUI();


            loadNetworkRoute();

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
            case R.id.clear_route_data:
                resetRoute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @UiThread
    protected void updateRouteUI() {
        updateCardsList();
        if (mRoute == null) {
            return;
        }

        if (getActionBar() != null) {
            getActionBar().setTitle(mRoute.name);
        }

        FragmentManager fm = this.getFragmentManager();
        MapFragment mMapFragment = (MapFragment) fm.findFragmentById(R.id.map);


        mMapFragment.getMap().setMyLocationEnabled(false);
        mMapFragment.getMap().setTrafficEnabled(false);
        mMapFragment.getMap().setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mRoute.latitude, mRoute.longitude), 14.0f));
        mMapFragment.getMap().addMarker(new MarkerOptions().position(new LatLng(mRoute.latitude, mRoute.longitude)).title(mRoute.name).snippet(mRoute.owner));
        mMapFragment.getMap().getUiSettings().setAllGesturesEnabled(false);

        updateShareIntent();
        updateAndroidBeamPayload();
    }

    private void updateCardsList() {
        mCardAdapter.clear();

        if (mRoute != null) {
            RouteDetailCard mRouteDetailCard = new RouteDetailCard(this, mCardAdapter, mRoute);
            RouteActionsCard mRouteActionsCard = new RouteActionsCard(this, this, mRoute);

            mCardAdapter.add(mRouteActionsCard);
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
        String shareTextRaw = getResources().getString(R.string.text_routedetail_share_text);
        String shareText = String.format(shareTextRaw, routeShare, AppConstants.SHARE_APPSTORE_LINK);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.setType("text/plain");

        mShareActionProvider.setShareIntent(shareIntent);
    }

    private void updateAndroidBeamPayload() {
        if (mNfcAdapter != null) {
            NdefRecord[] records = new NdefRecord[]{NdefRecord.createUri(AppConstants.SHARE_DOMAIN_NAME + AppConstants.SHARE_PATH_PREFIX + mRoute.id), NdefRecord.createApplicationRecord("de.happycarl.geotown.app")};

            NdefMessage mNdefMessage = new NdefMessage(records);

            mNfcAdapter.setNdefPushMessage(mNdefMessage, this);
        }
    }


    @Override
    public void onCheckBoxClicked(boolean status) {
        mRoute.starred = status;
        mRoute.save();
    }

    @Override
    public void onPlayButtonClicked() {
        SharedPreferences pref = GeotownApplication.getPreferences();
        final SharedPreferences.Editor editor = GeotownApplication.getPreferences().edit();
        long routeInPrefs = pref.getLong(AppConstants.PREF_CURRENT_ROUTE, 0L);
        if (routeInPrefs > 0L) { //There is a route preselected due to co-op mode
            if(routeInPrefs == routeId) {
                startPlayingRoute(false);
                return;
            }

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

                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setMessage(R.string.message_routedetail_cancel_current_route)
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setNegativeButton(android.R.string.no, dialogClickListener).show();

        } else { //No current Route
            startPlayingRoute(true);
        }

        updateRouteUI();
    }

    private void startPlayingRoute(boolean resetWaypoint) {
        SharedPreferences.Editor editor = GeotownApplication.getPreferences().edit();
        editor.putLong(AppConstants.PREF_CURRENT_ROUTE, mRoute.id);
        if(resetWaypoint)
            editor.putLong(AppConstants.PREF_CURRENT_WAYPOINT, 0L);
        editor.apply();

        //if route was played before, reset it now
        resetRoute();

        ((GeotownApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory(("InGame"))
                .setAction("Route Started")
                .build());

        PlayingActivity_.intent(this).start();
    }

    //================================================================================
    // Network
    //================================================================================

    private void loadNetworkRoute() {
        GeotownApplication.getJobManager().addJob(new RouteRequest(routeId));
        setProgressBarIndeterminateVisibility(true);
        loadLocalRoute();
    }

    @Background
    protected void loadLocalRoute() {
        mRoute = new Select()
                .from(GeoTownRoute.class)
                .where("routeID = ?", routeId)
                .limit(1)
                .executeSingle();

        updateRouteUI();
    }


    @Background
    protected void resetRoute() {
        try {
            ActiveAndroid.beginTransaction();

            for (GeoTownWaypoint wp : mRoute.waypoints()) {
                wp.done = false;
                wp.save();
            }
            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }

        loadLocalRoute();
    }

    public void onEvent(RouteDataReceivedEvent event) {
        if (event.route.getId() == routeId) {
            //Route is loaded and fully in db
            loadLocalRoute();
            setProgressBarIndeterminateVisibility(false);
        }
    }

}
