package de.happycarl.geotown.app.api.requests;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

/**
 * Created by olewe_000 on 15.09.2014.
 */
public abstract class NetworkRequestJob extends Job {


    public NetworkRequestJob(Params params) {
        super(params);
    }
    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        if (throwable instanceof java.net.SocketTimeoutException || throwable instanceof javax.net.ssl.SSLException)
            return true;
        return false;
    }
}
