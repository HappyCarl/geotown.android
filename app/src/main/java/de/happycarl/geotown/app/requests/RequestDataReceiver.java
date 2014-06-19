package de.happycarl.geotown.app.requests;

/**
 * Created by ole on 18.06.14.
 */
public interface RequestDataReceiver {
    //requests Stuff
    public static final int REQUEST_ALL_ROUTES = 0;
    public static final int REQUEST_USER_DATA = 1;
    public static final int REQUEST_ROUTE = 2;

    public void onRequestedData(int requestId, Object data);
}
