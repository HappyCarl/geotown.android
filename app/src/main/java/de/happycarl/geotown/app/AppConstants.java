package de.happycarl.geotown.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;

import com.appspot.drive_log.geotown.Geotown;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

import javax.annotation.Nullable;


/**
 * Created by ole on 18.06.14.
 */
public class AppConstants {


    //Appspot stuff
    //TODO: MOVE THIS TO A STRINGS FILE!!!!!
    public static final String CLIENT_ID = "server:client_id:1005962513631-78253fgvv2ahe6noj99iepmeccibtlvg.apps.googleusercontent.com";

    public static final JsonFactory JSON_FACTORY = new AndroidJsonFactory();
    public static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

    public static Geotown geoTownInstance;
    public static String userEmail;

    public static Geotown getApiServiceHandle(@Nullable GoogleAccountCredential credential) {
        Geotown.Builder gt = new Geotown.Builder(AppConstants.HTTP_TRANSPORT, AppConstants.JSON_FACTORY, credential);
        gt.setRootUrl("https://beta-dot-drive-log.appspot.com/_ah/api");
        gt.setApplicationName("GeoTown");
        return gt.build();
    }

    public static int countGoogleAccounts(Context context) {
        AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        if (accounts == null || accounts.length < 1) {
            return 0;
        } else {
            return accounts.length;
        }
    }

    public static boolean checkGooglePlayServicesAvailable(Activity activity) {
        final int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
            showGooglePlayServicesAvailabilityErrorDialog(activity, status);
            return false;
        }
        return true;
    }

    public static void showGooglePlayServicesAvailabilityErrorDialog(final Activity activity,
                                                                     final int connectionStatusCode) {
        final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        connectionStatusCode, activity, REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }


    //shared prefs stuff
    public static final String PREF_NAME = "GeoTown";
    public static final String PREF_ACCOUNT_NAME = "account_name";



}
