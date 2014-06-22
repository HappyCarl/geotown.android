package de.happycarl.geotown.app.gui.views;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.cardsui.CardAdapter;
import com.afollestad.cardsui.CardBase;

import de.happycarl.geotown.app.R;

/**
 * Created by jhbruhn on 22.06.14.
 */
public class RouteCardAdapter extends CardAdapter {
    public RouteCardAdapter(Context context, int accentColorRes) {
        super(context, accentColorRes);

        registerLayout(R.layout.card_loading);
        registerLayout(R.layout.card_progress);
    }

    @Override
    public View onViewCreated(int index, View recycled, CardBase item) {
        View view = super.onViewCreated(index, recycled, item);

        if (item instanceof ProgressCard) {
            ProgressCard pc = (ProgressCard) item;
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress);
            if (progressBar != null) {
                pc.setProgressBar(progressBar);

            }
            TextView textView = (TextView) view.findViewById(R.id.progress_text);
            if (textView != null) {
                pc.setProgressText(textView);
            }
        }
        return view;
    }
}
