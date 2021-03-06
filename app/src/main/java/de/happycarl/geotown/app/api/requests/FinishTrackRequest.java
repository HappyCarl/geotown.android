package de.happycarl.geotown.app.api.requests;

import android.util.Log;

import com.appspot.drive_log.geotown.Geotown;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.path.android.jobqueue.Params;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.entity.mime.HttpMultipartMode;
import ch.boye.httpclientandroidlib.entity.mime.MultipartEntity;
import ch.boye.httpclientandroidlib.entity.mime.content.ByteArrayBody;
import ch.boye.httpclientandroidlib.entity.mime.content.StringBody;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.ApiUtils;
import de.happycarl.geotown.app.events.net.TrackFinishedEvent;

/**
 * Created by ole on 28.09.14.
 */
public class FinishTrackRequest extends NetworkRequestJob {

    final long trackId;
    final String gpxFile;

    public FinishTrackRequest(long trackId, String gpxFile) {
        super(new Params(1).requireNetwork().persist().groupBy("finish-track"));

        this.gpxFile = gpxFile;
        this.trackId = trackId;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

        boolean success = true;

        GoogleAccountCredential cred = GoogleAccountCredential.usingAudience(GeotownApplication.getContext(), GeotownApplication.getContext().getResources().getString(R.string.client_id));
        cred.setSelectedAccountName(GeotownApplication.getPreferences().getString(AppConstants.PREF_ACCOUNT_EMAIL, ""));
        Geotown gt = ApiUtils.getApiServiceHandle(cred);

        String uploadUrl = gt.tracks().getTrackGPXUploadURL().execute().getUploadUrl();
        Log.d("Finish", "File:" + gpxFile);
        Log.d("FinishTrackRequest", "Got upload url: " + uploadUrl);

        if (uploadUrl != null && !uploadUrl.isEmpty()) {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost postRequest = new HttpPost(uploadUrl);
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            try {
                byte[] data = readFile(new File(gpxFile));
                ByteArrayBody bab = new ByteArrayBody(data, new File(gpxFile).getName());
                reqEntity.addPart("gpx", bab);
            }
            catch(Exception e) {
                e.printStackTrace();
                reqEntity.addPart("gpx", new StringBody(""));
            }
            postRequest.setEntity(reqEntity);
            HttpResponse response = httpClient.execute(postRequest);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            String sResponse;
            StringBuilder s = new StringBuilder();
            while ((sResponse = reader.readLine()) != null) {
                s = s.append(sResponse);
            }
            String res = s.toString();

            Log.d("FinishTrackRequest", "Response: " + res);
            JSONObject finalResult = new JSONObject(res);

            Log.d("FinishTrackRequest", "Result: " + finalResult.toString());

            String blobKey = finalResult.getString("blobkey");
            if (blobKey != null) {
                Log.d("FinishTrackRequest", "BlobKey: " + blobKey);
                gt.tracks().finishTrack(blobKey, trackId).execute();
            } else {
                success = false;
            }
        } else {
            success = false;
        }

        final boolean finSuccess = success;

        GeotownApplication.mHandler.post(new Runnable() {
            @Override
            public void run() {
                GeotownApplication.getEventBus().post(new TrackFinishedEvent(finSuccess));
            }
        });


    }

    public static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }

    @Override
    protected void onCancel() {

    }
}
