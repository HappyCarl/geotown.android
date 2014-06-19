package de.happycarl.geotown.app.requests;

import android.os.AsyncTask;

import com.appspot.drive_log.geotown.model.UserData;

import java.io.IOException;

import de.happycarl.geotown.app.AppConstants;

/**
 * Created by ole on 19.06.14.
 */
public class CurrentUserDataRequest extends AsyncTask<Void, Void, UserData> {

    RequestDataReceiver requestDataReceiver;

    public CurrentUserDataRequest(RequestDataReceiver receiver) {
        this.requestDataReceiver = receiver;
    }

    @Override
    protected UserData doInBackground(Void... params) {
        UserData userData = null;

        try {
            userData = AppConstants.geoTownInstance.geoTownEndpoints().getCurrentUserData().execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return userData;
    }

    @Override
    protected void onPostExecute(UserData userData) {
        requestDataReceiver.onRequestedData(AppConstants.REQUEST_USER_DATA, userData);
    }
}
