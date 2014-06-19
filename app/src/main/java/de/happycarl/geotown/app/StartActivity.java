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

import com.appspot.drive_log.geotown.model.Route;
import com.appspot.drive_log.geotown.model.RouteCollection;
import com.appspot.drive_log.geotown.model.UserData;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.ButterKnife;
import butterknife.OnClick;
import de.happycarl.geotown.app.requests.CurrentUserDataRequest;
import de.happycarl.geotown.app.requests.RequestDataReceiver;


public class StartActivity extends Activity implements RequestDataReceiver {

    static final int REQUEST_ACCOUNT_PICKER = 2;


    SharedPreferences settings;
    GoogleAccountCredential credential;
    String accountName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ButterKnife.inject(this);
        initSystemBarTint();
    }

    @OnClick(R.id.start_button)
    protected void loginGoogle() {
        if (!AppConstants.checkGooglePlayServicesAvailable(this))
            return;
        settings = getSharedPreferences(
                AppConstants.PREF_NAME, 0);
        credential = GoogleAccountCredential.usingAudience(this,
                AppConstants.CLIENT_ID);
        ;

        setSelectedAccountName(settings.getString(AppConstants.PREF_ACCOUNT_NAME, null));
        AppConstants.geoTownInstance = AppConstants.getApiServiceHandle(credential);

        if (credential.getSelectedAccountName() != null) {
            //Already signed in
            Log.d("Login", "Successfully logged in");
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
            CurrentUserDataRequest req = new CurrentUserDataRequest(this);
            req.execute((Void) null);
            //I somewhat should be redirecting people here...
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
        SharedPreferences.Editor editor = settings.edit();
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
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(AppConstants.PREF_ACCOUNT_NAME, accountName);
                        editor.commit();
                        // User is authorized.
                        loginGoogle();
                    }
                }
                break;
        }
    }


    @Override
    public void onRequestedData(int requestId, Object data) {
        switch (requestId) {
            case AppConstants.REQUEST_ALL_ROUTES:
                if (data == null) {
                    Toast.makeText(this, "No data from server", Toast.LENGTH_LONG).show();
                    return;
                }
                RouteCollection rc = (RouteCollection) data;

                if (rc.getItems() == null) {
                    Log.d("Routes", "no routes");
                    Toast.makeText(this, "You have no routes", Toast.LENGTH_LONG).show();
                    return;
                }


                for (Route r : rc.getItems()) {
                    String msg = r.getName() + " : " + r.getLatitude() + "/" + r.getLongitude();
                    Log.i("Routes", msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
                break;

            case AppConstants.REQUEST_USER_DATA:
                if (data == null) {
                    Toast.makeText(this, "No data from server", Toast.LENGTH_LONG).show();
                    return;
                }
                UserData userData = (UserData) data;

                Toast.makeText(this, userData.getEmail() + " : " + userData.getRoutes().size() + " Routes", Toast.LENGTH_LONG).show();

                break;
        }

    }
}
