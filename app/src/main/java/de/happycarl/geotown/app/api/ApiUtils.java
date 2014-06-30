package de.happycarl.geotown.app.api;

import com.appspot.drive_log.geotown.Geotown;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

import javax.annotation.Nullable;

/**
 * Created by jhbruhn on 19.06.14.
 */
public class ApiUtils {
    private static final JsonFactory JSON_FACTORY = new AndroidJsonFactory();
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

    public static Geotown getApiServiceHandle(@Nullable GoogleAccountCredential credential) {
        Geotown.Builder gt = new Geotown.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential);
        gt.setRootUrl("https://beta-dot-drive-log.appspot.com/_ah/api");
        gt.setApplicationName("GeoTown");
        return gt.build();
    }
}
