package de.happycarl.geotown.app.gui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Window;
import android.view.WindowManager;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.GameHelper;
import de.happycarl.geotown.app.events.google.GoogleClientConnectedEvent;
import de.happycarl.geotown.app.events.google.GoogleClientConnectionFailedEvent;

/**
 * Created by jhbruhn on 20.06.14.
 */
public abstract class SystemBarTintActivity extends ActionBarActivity {

    protected GameHelper mGameHelper;
    protected Toolbar mToolbar;

    protected void onCreate(Bundle savedInstanceState, int layoutId, boolean tintStatusBar) {
        if(tintStatusBar)
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
        super.onCreate(savedInstanceState);

        mGameHelper.setup(listener);
        setContentView(layoutId);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }
    }

    protected void onCreate(Bundle savedInstanceState, int layoutId) {
        onCreate(savedInstanceState, layoutId, true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGameHelper != null)
            mGameHelper.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGameHelper != null)
            mGameHelper.onStop();
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        if(mGameHelper != null)
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

    protected void setStatusBarTintColor(int color) {
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setTintColor(color);
    }

    protected void onSignInSucceeded() {
    }

    protected void onSignInFailed() {
    }


}
