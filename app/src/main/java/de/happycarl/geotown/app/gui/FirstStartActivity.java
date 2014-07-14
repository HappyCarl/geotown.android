package de.happycarl.geotown.app.gui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.happycarl.geotown.app.AppConstants;
import de.happycarl.geotown.app.GeotownApplication;
import de.happycarl.geotown.app.GoogleUtils;
import de.happycarl.geotown.app.R;
import de.happycarl.geotown.app.api.requests.CurrentUserDataRequest;
import de.happycarl.geotown.app.api.requests.SetUsernameRequest;
import de.happycarl.geotown.app.events.google.GoogleClientConnectedEvent;
import de.happycarl.geotown.app.events.net.UsernameSetEvent;


public class FirstStartActivity extends SystemBarTintActivity {
    //================================================================================
    // Constants
    //================================================================================

    static final int REQUEST_ACCOUNT_PICKER = 2;

    //================================================================================
    // Properties
    //================================================================================
    @InjectView(R.id.account_chooser_spinner)
    Spinner accountChooser;
    @InjectView(R.id.username_edit_text)
    EditText usernameEditText;

    private GoogleAccountCredential credential;
    private String accountName;
    private boolean requestedAccountPicker = false;
    private ProgressDialog progressDialog;

    //================================================================================
    // Activity Lifecycle
    //================================================================================
    private View.OnTouchListener spinnerTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            FirstStartActivity.this.accountChooserClicked();
            return true;
        }
    };
    private View.OnKeyListener spinnerKeyListener = new View.OnKeyListener() {

        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            FirstStartActivity.this.accountChooserClicked();
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


    //================================================================================
    // UI
    //================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ButterKnife.inject(this);
        GeotownApplication.getEventBus().register(this);

        accountChooser.setOnTouchListener(spinnerTouchListener);
        accountChooser.setOnKeyListener(spinnerKeyListener);
        usernameEditText.addTextChangedListener(usernameEditTextListener);

        credential = GoogleAccountCredential.usingAudience(this, AppConstants.CLIENT_ID);

        Account[] list = credential.getAllAccounts();
        setAccountName(list[0].name);


        String storedAccountName = GeotownApplication.getPreferences().getString(AppConstants.PREF_ACCOUNT_EMAIL, "");
        if (!storedAccountName.isEmpty()) {
            setSelectedAccountName(storedAccountName);

            ((GeotownApplication) getApplication()).login(credential);
            ((GeotownApplication) getApplication()).googleLogin(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GeotownApplication.getEventBus().unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ((GeotownApplication) getApplication()).getGameHelper().onActivityResult(requestCode, resultCode, data);
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

    private void startOverview() {
        //Already signed in
        Log.d("Login", "Successfully logged in");
        GeotownApplication.getJobManager().addJob(new CurrentUserDataRequest());

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

    //================================================================================
    // Networking
    //================================================================================

    @OnClick(R.id.start_button)
    protected void loginGoogle() {
        if (!GoogleUtils.checkGooglePlayServicesAvailable(this))
            return;
        if (usernameEditText.getText().toString().length() <= 0) {
            usernameEditText.setError(getString(R.string.error_no_username));
            return;
        }
        setSelectedAccountName(accountName);

        ((GeotownApplication) getApplication()).login(credential);


        if (credential.getSelectedAccountName() != null) {
            progressDialog = ProgressDialog.show(this, "", getString(R.string.loading));
            startSetUsernameRequest((usernameEditText.getText().toString().trim()));
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

    // setSelectedAccountName definition
    private void setSelectedAccountName(String accountName) {
        SharedPreferences.Editor editor = GeotownApplication.getPreferences().edit();
        editor.putString(AppConstants.PREF_ACCOUNT_EMAIL, accountName);
        editor.commit();
        credential.setSelectedAccountName(accountName);
        this.accountName = accountName;
    }

    private void startSetUsernameRequest(String name) {
        GeotownApplication.getJobManager().addJob(new SetUsernameRequest(name));
    }

    @Subscribe
    public void onUsernameSet(UsernameSetEvent e) {
        GeotownApplication.getPreferences().edit().putString(AppConstants.PREF_ACCOUNT_NAME, e.userData.getUsername()).apply();
        progressDialog.cancel();
        ((GeotownApplication) getApplication()).googleLogin(this);
    }

    @Subscribe
    public void onGoogleClientConnected(GoogleClientConnectedEvent e) {
        Log.i("PEDAB", "Sign In Arrived");
        if (GeotownApplication.getPreferences().getLong(AppConstants.PREF_CURRENT_ROUTE, -1L) != -1) {
            Intent playingActivity = new Intent(this, PlayingActivity.class);
            startActivity(playingActivity);
            finish();
        } else {
            startOverview();
        }
    }

    private void accountChooserClicked() {
        chooseAccount();
    }

}
