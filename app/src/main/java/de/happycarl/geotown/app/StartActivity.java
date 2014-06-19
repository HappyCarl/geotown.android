package de.happycarl.geotown.app;

import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.appspot.drive_log.geotown.model.UserData;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.OnClick;
import de.happycarl.geotown.app.api.requests.CurrentUserDataRequest;
import de.happycarl.geotown.app.events.CurrentUserDataReceivedEvent;
import de.happycarl.geotown.app.gui.OverviewActivity;


public class StartActivity extends Activity {

    static final int REQUEST_ACCOUNT_PICKER = 2;


    GoogleAccountCredential credential;
    String accountName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);
        initSystemBarTint();
    }

    @OnClick(R.id.start_button)
    protected void loginGoogle() {
        if (!GoogleUtils.checkGooglePlayServicesAvailable(this))
            return;

        credential = GoogleAccountCredential.usingAudience(this,
                AppConstants.CLIENT_ID);

        setSelectedAccountName(GeotownApplication.getPreferences().getString(AppConstants.PREF_ACCOUNT_NAME, null));
        GeotownApplication.login(credential);

        if (credential.getSelectedAccountName() != null) {
            //Already signed in
            Log.d("Login", "Successfully logged in");
            CurrentUserDataRequest req = new CurrentUserDataRequest();
            req.execute((Void) null);

            Intent overviewScreen = new Intent(this, OverviewActivity.class);
            startActivity(overviewScreen);
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
            finish();

        } else {
            Log.d("Login", "Showing account picker");
            chooseAccount();
        }


    }

    private void chooseAccount() {
        startActivityForResult(credential.newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER);
    }


    private void initSystemBarTint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setTranslucentStatus(true);
        }

        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setNavigationBarTintEnabled(true);
        tintManager.setStatusBarTintResource(R.color.actionbar_background_color);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Nobody needs a menu on the start screen
        //getMenuInflater().inflate(R.menu.start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // setSelectedAccountName definition
    private void setSelectedAccountName(String accountName) {
        SharedPreferences.Editor editor = GeotownApplication.getPreferences().edit();
        editor.putString(AppConstants.PREF_ACCOUNT_NAME, accountName);
        editor.commit();
        credential.setSelectedAccountName(accountName);
        this.accountName = accountName;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (data != null && data.getExtras() != null) {
                    String accountName =
                            data.getExtras().getString(
                                    AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        setSelectedAccountName(accountName);
                        SharedPreferences.Editor editor = GeotownApplication.getPreferences().edit();
                        editor.putString(AppConstants.PREF_ACCOUNT_NAME, accountName);
                        editor.commit();
                        // User is authorized.
                        loginGoogle();
                    }
                }
                break;
        }
    }

    @Subscribe
    public void onCurrentUserDataReceived(CurrentUserDataReceivedEvent event) {
        UserData data = event.userData;
        if (data == null) {
            Toast.makeText(this, "No data from server", Toast.LENGTH_LONG).show();
            return;
        }
        UserData userData = (UserData) data;

        Toast.makeText(this, "Logged in as " + userData.getEmail(), Toast.LENGTH_LONG).show();

    }

}
