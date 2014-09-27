package de.happycarl.geotown.app.gui.data;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.activeandroid.query.Select;
import com.afollestad.cardsui.CardAdapter;
import com.afollestad.cardsui.CardBase;
import com.afollestad.cardsui.CardCenteredHeader;
import com.afollestad.cardsui.CardHeader;
import com.google.common.collect.Lists;

import java.util.List;

import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.gui.views.ProgressCard;
import de.happycarl.geotown.app.gui.views.RouteCard;
import de.happycarl.geotown.app.models.GeoTownRoute;

/**
 * Created by jhbruhn on 29.06.14 for geotown.android.
 */
public class OverviewCardsAdapter extends CardAdapter {
    private List<GeoTownRoute> nearRoutes;
    private List<GeoTownRoute> myRoutes;
    private List<GeoTownRoute> localRoutes;

    private GeoTownRoute currentRoute;

    private final CardHeader nearRoutesHeader = new CardHeader(getContext().getString(R.string.title_overview_near_routes));
    private final CardHeader localRoutesHeader = new CardHeader(getContext().getString(R.string.title_overview_local_routes));
    private final CardHeader myRoutesHeader = new CardHeader(getContext().getString(R.string.title_overview_my_routes));

    private final CardCenteredHeader noOwnRoutesHeader = new CardCenteredHeader(getContext().getString(R.string.message_overview_no_my_routes));
    private final CardCenteredHeader noLocalRoutesHeader = new CardCenteredHeader(getContext().getString(R.string.message_overview_no_my_routes));
    private final CardCenteredHeader noNearRoutesHeader = new CardCenteredHeader(getContext().getString(R.string.message_overview_no_near_routes));

    private ProgressCard progressCard;

    private final Handler handler;

    public OverviewCardsAdapter(Context context) {
        super(context, R.color.primary_color);

        registerLayout(R.layout.card_loading);
        registerLayout(R.layout.card_progress);

        handler = new Handler();

        this.add(nearRoutesHeader);
        this.add(localRoutesHeader);
        this.add(myRoutesHeader);

        startRefreshNearRoutes();
        startRefreshSavedRoutes();
        startRefreshMyRoutes();
        startRefreshCurrentRoute();
    }

    @Override
    public View onViewCreated(int index, View recycled, CardBase item) {
        View view = super.onViewCreated(index, recycled, item);
        if (item instanceof ProgressCard) {
            ProgressCard pc = (ProgressCard) item;
            pc.updateView(view);
        }
        return view;
    }

    private void updateCurrentRoute() {
        if (currentRoute != null) {
            progressCard = new ProgressCard(this.getContext(), this, currentRoute);

            if(this.getItem(0) instanceof ProgressCard)
                this.remove(0);

            this.add(0, progressCard);

        } else {
            if (progressCard != null) {
                this.remove(progressCard);
                progressCard = null;
            }
        }
    }

    private void updateNearRoutes() {
        int nearRoutesIndex = this.getItems().indexOf(nearRoutesHeader) + 1;

        while (!(getItem(nearRoutesIndex) instanceof CardHeader) && getItem(nearRoutesIndex) != null) {
            this.remove(nearRoutesIndex);
        }

        if (nearRoutes == null || nearRoutes.size() == 0) {
            this.add(nearRoutesIndex, noNearRoutesHeader);
            return;
        } else {
            int noNearRoutesIndex = this.getItems().indexOf(noNearRoutesHeader);
            if (noNearRoutesIndex >= 0)
                this.remove(noNearRoutesIndex);
        }

        for (GeoTownRoute r : Lists.reverse(nearRoutes)) {
            this.add(nearRoutesIndex, new RouteCard(this.getContext(), this, r));
        }
    }

    private void updateLocalRoutes() {
        int localRoutesIndex = this.getItems().indexOf(localRoutesHeader) + 1;

        while (!(getItem(localRoutesIndex) instanceof CardHeader) && getItem(localRoutesIndex) != null) {
            this.remove(localRoutesIndex);
        }
        if (localRoutes == null || localRoutes.size() == 0) {
            this.add(localRoutesIndex, noLocalRoutesHeader);
            return;
        } else {
            int noLocalRoutesIndex = this.getItems().indexOf(noLocalRoutesHeader);
            if (noLocalRoutesIndex >= 0)
                this.remove(noLocalRoutesIndex);
        }

        for (GeoTownRoute r : localRoutes) {
            Log.d("PEDA", r.name);
            this.add(localRoutesIndex++, new RouteCard(this.getContext(), this, r));
        }
    }

    private void updateMyRoutes() {
        int myRoutesIndex = this.getItems().indexOf(myRoutesHeader) + 1;

        while (myRoutesIndex < getItems().size() && !(getItem(myRoutesIndex) instanceof CardHeader) && getItem(myRoutesIndex) != null) {
            this.remove(myRoutesIndex);
        }

        if (myRoutes == null || myRoutes.size() == 0) {
            this.add(myRoutesIndex++, noOwnRoutesHeader);
            return;
        } else {
            int noOwnRoutesIndex = this.getItems().indexOf(noOwnRoutesHeader);
            if (noOwnRoutesIndex >= 0)
                this.remove(noOwnRoutesIndex);
        }

        for (GeoTownRoute r : myRoutes) {
            this.add(myRoutesIndex++, new RouteCard(this.getContext(), this, r));
        }
    }

    public void setLocalRoutes(List<GeoTownRoute> localRoutes) {
        this.localRoutes = localRoutes;
        this.updateLocalRoutes();
        this.updateCurrentRoute();
    }

    private void setNearRoutes(List<GeoTownRoute> nearRoutes) {
        this.nearRoutes = nearRoutes;
        this.updateNearRoutes();
        this.updateCurrentRoute();
    }

    public void setMyRoutes(List<GeoTownRoute> myRoutes) {
        this.myRoutes = myRoutes;
        this.updateMyRoutes();
        this.updateCurrentRoute();
    }

    private void setCurrentRoute(GeoTownRoute route) {
        this.currentRoute = route;
        this.updateCurrentRoute();
        this.updateCurrentRoute();
    }

    public void startRefreshNearRoutes() {
        new GetNearRoutesTask().execute();
    }

    public void startRefreshSavedRoutes() {
        new GetSavedRoutesTask().execute();
    }

    public void startRefreshMyRoutes() {
        new GetMyRoutesTask().execute();
    }

    public void startRefreshCurrentRoute() {
        new GetCurrentRouteTask().execute();
    }

    private class GetNearRoutesTask extends AsyncTask<Void, Void, List<GeoTownRoute>> {

        @Override
        protected List<GeoTownRoute> doInBackground(Void... peda) {
            return new Select()
                    .from(GeoTownRoute.class)
                    .orderBy("nearIndex ASC")
                    .limit(3)
                    .execute();
        }

        @Override
        protected void onPostExecute(final List<GeoTownRoute> geoTownRoutes) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    OverviewCardsAdapter.this.setNearRoutes(geoTownRoutes);
                }
            });
        }
    }

    private class GetSavedRoutesTask extends AsyncTask<Void, Void, List<GeoTownRoute>> {

        @Override
        protected List<GeoTownRoute> doInBackground(Void... peda) {
            return new Select()
                    .from(GeoTownRoute.class)
                    .where("starred = ?", true)
                    .execute();
        }

        @Override
        protected void onPostExecute(final List<GeoTownRoute> geoTownRoutes) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    OverviewCardsAdapter.this.setLocalRoutes(geoTownRoutes);
                }
            });
        }
    }

    private class GetMyRoutesTask extends AsyncTask<Void, Void, List<GeoTownRoute>> {

        @Override
        protected List<GeoTownRoute> doInBackground(Void... peda) {
            return new Select()
                    .from(GeoTownRoute.class)
                    .where("mine = ?", true)
                    .execute();
        }

        @Override
        protected void onPostExecute(final List<GeoTownRoute> geoTownRoutes) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    OverviewCardsAdapter.this.setMyRoutes(geoTownRoutes);
                }
            });

        }
    }

    private class GetCurrentRouteTask extends AsyncTask<Void, Void, GeoTownRoute> {

        @Override
        protected GeoTownRoute doInBackground(Void... peda) {
            long currentRouteId = GeotownApplication.getPreferences().getLong(AppConstants.PREF_CURRENT_ROUTE, 0L);
            if (currentRouteId != 0) {
                return new Select()
                        .from(GeoTownRoute.class)
                        .where("routeID = ?", currentRouteId)
                        .limit(1)
                        .executeSingle();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final GeoTownRoute route) {
            Log.d("Ulf", route + "");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    OverviewCardsAdapter.this.setCurrentRoute(route);
                }
            });
        }
    }
}
