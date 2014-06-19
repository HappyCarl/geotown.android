package de.happycarl.geotown.app.gui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
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

import butterknife.ButterKnife;
import butterknife.InjectView;
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

        CardHeader header = new CardHeader(getResources().getString(R.string.my_routes));
        adapter.add(header);

        AllMyRoutesRequest routesRequest = new AllMyRoutesRequest();
        routesRequest.execute((Void) null);

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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    @SuppressWarnings("rawtypes unchecked")
    public void onMyRoutesDataReceived(MyRoutesDataReceivedEvent event) {
        if (event.routes == null || event.routes.size() == 0) {
            CardCenteredHeader empty = new CardCenteredHeader(getResources().getString(R.string.no_routes));
            adapter.add(empty);
        }
        for (Route r : event.routes) {
            Card c = new Card(r.getName(), r.getLatitude() + "/" + r.getLongitude());
            //TODO:Add image via Picasso
            adapter.add(c);
        }
    }


}
