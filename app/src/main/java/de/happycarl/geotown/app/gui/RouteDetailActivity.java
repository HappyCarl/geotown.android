package de.happycarl.geotown.app.gui;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;
import com.afollestad.cardsui.CardAdapter;
import com.afollestad.cardsui.CardListView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.InjectView;
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

public class RouteDetailActivity extends SystemBarTintActivity implements RouteActionsCard.RouteActionsCardListener {

    public static final int REQUEST_ROUTE_ID = 876354;

    //================================================================================
    // Properties
    //================================================================================

    @InjectView(R.id.route_detail_card_list)
    CardListView cardsList;

    private ShareActionProvider mShareActionProvider;
    private CardAdapter mCardAdapter;

    private long routeId = -1;
    private GeoTownRoute mRoute;

    private NfcAdapter mNfcAdapter;


    //================================================================================
    // Activity Lifecycle
    //================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detail);


        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);


        mCardAdapter = new RouteDetailCardAdapter(this, R.color.primary_color);
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

    @Override
    public void onStart() {
        super.onStart();
        GeotownApplication.getGameHelper().onStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        GeotownApplication.getGameHelper().onStop();
    }


    @Override
    protected void onActivityResult(int request, int response, Intent data) {

        GeotownApplication.getGameHelper().onActivityResult(request, response, data);
        super.onActivityResult(request, response, data);
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
            case R.id.clear_route_data:
                new ResetRouteTask().execute();
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
        String shareTextRaw = getResources().getString(R.string.share_text);
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
            editor.putLong(AppConstants.PREF_CURRENT_ROUTE, mRoute.id);
            editor.apply();

            startActivity(new Intent(this, PlayingActivity.class));
        }

        updateRouteUI();
    }

    //================================================================================
    // Network
    //================================================================================

    private void loadRoute() {
        GeotownApplication.getJobManager().addJob(new RouteRequest(routeId));

    }

    @Subscribe
    public void onRouteDataReceived(RouteDataReceivedEvent event) {
        if (event.route.getId() == routeId) {
            //Route is loaded and fully in db
            new LoadRouteTask().execute(routeId);
        }
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

    private class ResetRouteTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
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
            loadRoute();

            return null;
        }
    }

}
