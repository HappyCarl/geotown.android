package de.happycarl.geotown.app.api.requests;

import android.net.http.AndroidHttpClient;
import android.util.Log;

import com.path.android.jobqueue.Params;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.events.net.TrackFinishedEvent;

/**
 * Created by ole on 28.09.14.
 */
public class FinishTrackRequest extends NetworkRequestJob {

    long trackId;
    String gpxFile;

    public FinishTrackRequest(long trackId, String gpxFile) {
        super(new Params(3).requireNetwork().persist().groupBy("finish-track"));

        this.gpxFile = gpxFile;
        this.trackId = trackId;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

        boolean success = true;

        String uploadUrl = GeotownApplication.getGeotown().tracks().getTrackGPXUploadURL().execute().getUploadUrl();
        Log.d("FinishTrackRequest", "Got upload url: " + uploadUrl);

        if (uploadUrl != null && !uploadUrl.isEmpty()) {

            HttpClient httpClient = AndroidHttpClient.newInstance("GeoTown-App");
            HttpPost httpPost = new HttpPost(uploadUrl);

            List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("gpx", readGPXFile()));

            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

            HttpResponse response = httpClient.execute(httpPost);

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            JSONTokener tokener = new JSONTokener(builder.toString());
            JSONArray finalResult = new JSONArray(tokener);

            Log.d("FinishTrackRequest", "Result: " + finalResult.toString());

            String blobKey = finalResult.getString(0);
            if (blobKey != null) {
                Log.d("FinishTrackRequest", "BlobKey: " + blobKey);
                GeotownApplication.getGeotown().tracks().finishTrack(blobKey, trackId).execute();
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

    private String readGPXFile() {
        File file = new File(gpxFile);

        StringBuilder buf = new StringBuilder();

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = "";

            while ((line = br.readLine()) != null) {
                buf.append(line);
                buf.append('\n');
            }
            br.close();

        } catch (IOException e) {
            Log.e("FinishTrackRequest", "Read error: " + e.getMessage());
            return null;
        }

        return buf.toString();
    }

    @Override
    protected void onCancel() {

    }
}
