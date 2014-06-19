package de.happycarl.geotown.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.happycarl.geotown.app.api.requests.CurrentUserDataRequest;
import de.happycarl.geotown.app.api.requests.SetUsernameRequest;
import de.happycarl.geotown.app.events.CurrentUserDataReceivedEvent;
import de.happycarl.geotown.app.events.UsernameSetEvent;
import de.happycarl.geotown.app.gui.OverviewActivity;


public class StartActivity extends Activity {

    static final int REQUEST_ACCOUNT_PICKER = 2;


    GoogleAccountCredential credential;
    String accountName;

    @InjectView(R.id.account_chooser_spinner)
    Spinner accountChooser;

    @InjectView(R.id.username_edit_text)
    EditText usernameEditText;


    ProgressDialog progressDialog;


    private boolean requestedAccountPicker = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);
        initSystemBarTint();

        GeotownApplication.getEventBus().register(this);


        accountChooser.setOnTouchListener(spinnerTouchListener);
        accountChooser.setOnKeyListener(spinnerKeyListener);
        usernameEditText.addTextChangedListener(usernameEditTextListener);

        credential = GoogleAccountCredential.usingAudience(this, AppConstants.CLIENT_ID);

        Account[] list = credential.getAllAccounts();
        setAccountName(list[0].name);

        String storedAccountName = GeotownApplication.getPreferences().getString(AppConstants.PREF_ACCOUNT_NAME, "");
        if (!storedAccountName.isEmpty()) {
            setSelectedAccountName(storedAccountName);

            GeotownApplication.login(credential);
            startOverview();
        }
    }

    private void startOverview() {
        //Already signed in
        Log.d("Login", "Successfully logged in");
        CurrentUserDataRequest req = new CurrentUserDataRequest();
        req.execute((Void) null);

        Intent overviewScreen = new Intent(this, OverviewActivity.class);
        startActivity(overviewScreen);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
        finish();
    }

    private void updateAccountChooserValue(String accountName) {
        accountChooser.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{accountName}));
    }

    private void setAccountName(String accountName) {
        this.accountName = accountName;
        this.updateAccountChooserValue(accountName);
        credential.setSelectedAccountName(accountName);
    }

    @OnClick(R.id.start_button)
    protected void loginGoogle() {
        if (!GoogleUtils.checkGooglePlayServicesAvailable(this))
            return;
        if (usernameEditText.getText().toString().length() <= 0) {
            usernameEditText.setError(getString(R.string.error_no_username));
            return;
        }
        setSelectedAccountName(accountName);

        GeotownApplication.login(credential);


        if (credential.getSelectedAccountName() != null) {
            progressDialog = ProgressDialog.show(this, "", getString(R.string.loading));
            new SetUsernameRequest().execute(usernameEditText.getText().toString().trim());
        } else {
            Log.d("Login", "Showing account picker");
            chooseAccount();
        }


    }

    private void chooseAccount() {
        if (requestedAccountPicker) return;

        requestedAccountPicker = true;
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //So why would someone need an event responder
        return true;
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
                    requestedAccountPicker = false;
                    String accountName =
                            data.getExtras().getString(
                                    AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        setSelectedAccountName(accountName);
                    }
                }
                break;
        }
    }

    @Subscribe
    public void onCurrentUserDataReceived(CurrentUserDataReceivedEvent event) {


    }

    @Subscribe
    public void onUsernameSet(UsernameSetEvent e) {
        progressDialog.hide();
        startOverview();
    }


    private void accountChooserClicked() {
        chooseAccount();
    }


    private View.OnTouchListener spinnerTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            StartActivity.this.accountChooserClicked();
            return true;
        }
    };

    private View.OnKeyListener spinnerKeyListener = new View.OnKeyListener() {

        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            StartActivity.this.accountChooserClicked();
            return true;
        }
    };

    private TextWatcher usernameEditTextListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (usernameEditText.getText().toString().length() <= 0)
                usernameEditText.setError(getString(R.string.error_no_username));
        }
    };

}
