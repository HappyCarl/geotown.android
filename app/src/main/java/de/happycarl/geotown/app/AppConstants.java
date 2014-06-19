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

    //shared prefs stuff
    public static final String PREF_NAME = "GeoTown";
    public static final String PREF_ACCOUNT_NAME = "account_name";



}
