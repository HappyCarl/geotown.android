package de.happycarl.geotown.app.gui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.afollestad.cardsui.Card;
import com.afollestad.cardsui.CardAdapter;
import com.afollestad.cardsui.CardCenteredHeader;
import com.afollestad.cardsui.CardHeader;
import com.afollestad.cardsui.CardListView;
import com.appspot.drive_log.geotown.model.Route;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.requests.AllMyRoutesRequest;
import de.happycarl.geotown.app.events.MyRoutesDataReceivedEvent;

public class OverviewActivity extends Activity {

    @InjectView(R.id.route_view)
    CardListView cardListView;

    @InjectView(R.id.overview_user)
    TextView userText;



    CardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);

        String userEmail = GeotownApplication.getPreferences().getString(AppConstants.PREF_ACCOUNT_NAME, "");
        userText.setText(Html.fromHtml("<i>" + userEmail + "</i>"));


        adapter = new CardAdapter(this, android.R.color.holo_red_light);
        cardListView.setAdapter(adapter);

        AllMyRoutesRequest routesRequest = new AllMyRoutesRequest();
        routesRequest.execute((Void) null);

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
                adapter.clear();
                AllMyRoutesRequest routesRequest = new AllMyRoutesRequest();
                routesRequest.execute((Void) null);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    @SuppressWarnings("rawtypes unchecked")
    public void onMyRoutesDataReceived(MyRoutesDataReceivedEvent event) {
        if (event.routes == null || event.routes.size() == 0) {
            CardCenteredHeader empty = new CardCenteredHeader(getResources().getString(R.string.no_routes));
            adapter.add(empty);
            return;
        }



        CardHeader header = new CardHeader(getResources().getString(R.string.my_routes));
        adapter.add(header);
        for (Route r : event.routes) {


            Card c = new Card(r.getName(), getLocationName(r.getLatitude(), r.getLongitude()));
            //TODO:Add image via Picasso
            CardTarget target = new CardTarget(c,this,adapter);
            Picasso.with(this).load("https://maps.google.com/maps/api/staticmap?center=" + r.getLatitude() + ","+r.getLongitude()+"&size=128x128&zoom=8").into(target);
            

        }
    }

    private String getLocationName(double latitude, double longitude) {
        String result = getResources().getString(R.string.unknown_place);

        Geocoder gc = new Geocoder(this);

        try {
            Address address = gc.getFromLocation(latitude, longitude, 1).get(0);

            String town = getResources().getString(R.string.unknown_town);
            if(address.getLocality()!=null) {
                town = address.getLocality();
            }
            String country = getResources().getString(R.string.unknown_country);
            if(address.getCountryName() != null) {
                country = address.getCountryName();
            }
            result = town + ", " + country;
        } catch (IOException | IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return result;
    }




}
