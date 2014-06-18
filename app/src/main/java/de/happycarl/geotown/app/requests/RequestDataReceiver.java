package de.happycarl.geotown.app.requests;

/**
 * Created by ole on 18.06.14.
 */
public interface RequestDataReceiver {

    public void onRequestedData(int requestId, Object data);
}
