package de.happycarl.geotown.app.licenses;

import android.content.Context;

import de.psdev.licensesdialog.licenses.License;

/**
 * Created by ole on 19.10.14.
 */
public class GeoTownLicense extends License {
    @Override
    public String getName() {
        return "GeoTown";
    }

    @Override
    public String getSummaryText(Context context) {
        return "This app is brought to you by the HappyCarl dev team. Credits for the project go to\n" +
                "  - Jan-Henrik Bruhn (github.com/jhbruhn)\n" +
                "  - Ole Wehrmeyer (github.com/belogron)\n\n" +
                "Special thanks for the icon & graphic design goes to onekonek01.";
    }

    @Override
    public String getFullText(Context context) {
        return null;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getUrl() {
        return "geotown.de";
    }
}
