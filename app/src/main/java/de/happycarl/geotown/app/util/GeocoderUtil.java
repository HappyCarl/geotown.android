package de.happycarl.geotown.app.util;

import android.content.Context;
import android.location.Address;

import java.io.IOException;

import de.happycarl.geotown.app.R;

/**
 * Created by jhbruhn on 28.09.14 for geotown.android.
 */
public class GeocoderUtil {
    public static String geocodeLocation(double lat, double lng, Context ctx) {
        GoogleGeocoder gc = new GoogleGeocoder(ctx);
        String result = "";
        try {
            Address address = gc.getFromLocation(lat, lng, 1).get(0);

            String town = ctx.getResources().getString(R.string.unknown_town);
            if (address.getLocality() != null) {
                town = address.getLocality();
            }
            String country = ctx.getResources().getString(R.string.unknown_country);
            if (address.getCountryName() != null) {
                country = address.getCountryName();
            }
            result = town + ", " + country;
        } catch (IOException | IndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (GoogleGeocoder.LimitExceededException e) {
            e.printStackTrace();
        }

        return result;
    }
}
