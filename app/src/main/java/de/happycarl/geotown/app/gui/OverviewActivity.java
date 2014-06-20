package de.happycarl.geotown.app.gui;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.afollestad.cardsui.CardAdapter;
import com.afollestad.cardsui.CardCenteredHeader;
import com.afollestad.cardsui.CardHeader;
import com.afollestad.cardsui.CardListView;
import com.appspot.drive_log.geotown.model.Route;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.GoogleUtils;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.requests.AllMyRoutesRequest;
import de.happycarl.geotown.app.api.requests.NearRoutesRequest;
import de.happycarl.geotown.app.events.MyRoutesDataReceivedEvent;
import de.happycarl.geotown.app.events.NearRoutesDataReceivedEvent;
import de.happycarl.geotown.app.gui.views.RouteCard;
import de.happycarl.geotown.app.models.GeoTownRoute;

public class OverviewActivity extends Activity {

    @InjectView(R.id.route_view)
    CardListView cardListView;

    @InjectView(R.id.overview_loading_layout)
    LinearLayout loadingLayout;

    @InjectView(R.id.overview_card_layout)
    LinearLayout cardUILayout;

    CardAdapter adapter;

    private List<Route> myRoutes = new ArrayList<Route>();
    private List<Route> nearRoutes = new ArrayList<Route>();
    boolean loadingMyRoutes = false;
    boolean loadingNearRoutes = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);

        adapter = new CardAdapter(this, R.color.primary_color);
        cardListView.setAdapter(adapter);

        refreshRoutes();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.overview, menu);
        return true;
    }


    protected void exitButton() {
        finish();
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
            case R.id.action_refresh:
                refreshRoutes();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshRoutes() {
        loadingLayout.setVisibility(View.VISIBLE);
        cardUILayout.setVisibility(View.GONE);

        AllMyRoutesRequest routesRequest = new AllMyRoutesRequest();
        routesRequest.execute((Void) null);
        loadingMyRoutes = true;

        NearRoutesRequest nearRoutesRequest = new NearRoutesRequest();
        //TODO: get value from GPS.
        nearRoutesRequest.execute(new NearRoutesRequest.NearRoutesParams(52, 8, 10000000));
        loadingNearRoutes = true;
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

    @SuppressWarnings("rawtypes unchecked")
    private void updateCardsUI() {
        adapter.clear();

        // Near ROutes
        CardHeader header = new CardHeader(getResources().getString(R.string.near_routes));
        header.setClickable(true);
        header.setAction(getResources().getString(R.string.see_more), nearRoutesActionListener);
        adapter.add(header);

        if (nearRoutes == null || nearRoutes.size() == 0) {
            CardCenteredHeader empty = new CardCenteredHeader(getResources().getString(R.string.no_near_routes));
            adapter.add(empty);
        }

        if (nearRoutes != null) {
            int i = 0; // Only show 3 near routes.
            for (Route r : nearRoutes) {
                if (i >= 3) break;
                RouteCard c = new RouteCard(this, adapter, r.getName(), getLocationName(r.getLatitude(), r.getLongitude()));
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
                RouteCard c = new RouteCard(this, adapter, r.getName(), getLocationName(r.getLatitude(), r.getLongitude()));
                Picasso.with(this).load(GoogleUtils.getStaticMapUrl(r.getLatitude(), r.getLongitude(), 8, 128)).placeholder(R.drawable.ic_launcher).into(c);

                GeoTownRoute.update(r, true);
            }
        }
        loadingLayout.setVisibility(View.GONE);
        cardUILayout.setVisibility(View.VISIBLE);

    }

    private String getLocationName(double latitude, double longitude) {
        String result = getResources().getString(R.string.unknown_place);

        Geocoder gc = new Geocoder(this);

        try {
            Address address = gc.getFromLocation(latitude, longitude, 1).get(0);

            String town = getResources().getString(R.string.unknown_town);
            if (address.getLocality() != null) {
                town = address.getLocality();
            }
            String country = getResources().getString(R.string.unknown_country);
            if (address.getCountryName() != null) {
                country = address.getCountryName();
            }
            result = town + ", " + country;
        } catch (IOException | IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return result;
    }

    private CardHeader.ActionListener nearRoutesActionListener = new CardHeader.ActionListener() {

        @Override
        public void onHeaderActionClick(CardHeader cardHeader) {
            // TODO: Show "NearRoutesActivity" here.
        }
    };


}
