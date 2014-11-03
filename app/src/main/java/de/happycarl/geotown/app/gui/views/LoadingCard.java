package de.happycarl.geotown.app.gui.views;

import com.afollestad.cardsui.Card;

import de.happycarl.geotown.app.R;

/**
 * Created by jhbruhn on 22.06.14.
 */
public class LoadingCard extends Card {
    public LoadingCard() {
        super("");
    }

    public int getLayout() {
        // Replace with your toolbar
        return R.layout.card_loading;
    }
}
