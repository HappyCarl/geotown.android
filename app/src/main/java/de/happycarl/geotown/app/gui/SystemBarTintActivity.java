package de.happycarl.geotown.app.gui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.appspot.drive_log.geotown.Geotown;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.GameHelper;
import de.happycarl.geotown.app.events.google.GoogleClientConnectedEvent;
import de.happycarl.geotown.app.events.google.GoogleClientConnectionFailedEvent;

/**
 * Created by jhbruhn on 20.06.14.
 */
public abstract class SystemBarTintActivity extends Activity {

    protected GameHelper mGameHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initSystemBarTint();

        // create game helper with all APIs (Games and Plus):
        mGameHelper = new GameHelper(this, GameHelper.CLIENT_GAMES);


        GameHelper.GameHelperListener listener = new GameHelper.GameHelperListener() {
            @Override
            public void onSignInSucceeded() {
                GeotownApplication.getEventBus().post(new GoogleClientConnectedEvent());
                SystemBarTintActivity.this.onSignInSucceeded();
            }
            @Override
            public void onSignInFailed() {
                GeotownApplication.getEventBus().post(new GoogleClientConnectionFailedEvent());
                SystemBarTintActivity.this.onSignInFailed();
            }

        };
        mGameHelper.setup(listener);

        super.onCreate(savedInstanceState);
    }

    protected void onSignInSucceeded() {

    }

    protected void onSignInFailed() {

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGameHelper.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGameHelper.onStop();
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        mGameHelper.onActivityResult(request, response, data);
    }

    @TargetApi(19)
    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    private void initSystemBarTint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setTranslucentStatus(true);
        }

        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setNavigationBarTintEnabled(true);
        tintManager.setStatusBarTintResource(R.color.primary_color);
    }


}
