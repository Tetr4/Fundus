package de.hundebarf.fundus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import de.hundebarf.fundus.model.FundusAccount;
import de.hundebarf.fundus.service.ServiceConnection;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LoginActivity extends Activity {
    private EditText mUserEditText;
    private EditText mPasswordEditText;

    private FundusAccount mGuestAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mGuestAccount = new FundusAccount(getString(R.string.guest_user), getString(R.string.guest_password));

        Button mLoginButton = (Button) findViewById(R.id.button_login);
        Button mGuestButton = (Button) findViewById(R.id.button_guest);
        mUserEditText = (EditText) findViewById(R.id.edittext_user);
        mPasswordEditText = (EditText) findViewById(R.id.edittext_password);

        // fill with last user/password
        FundusApplication app = (FundusApplication) getApplication();
        FundusAccount account = app.getAccount();
        mUserEditText.setText(account.getUser());
        mUserEditText.setSelection(mUserEditText.getText().length()); // move cursor
        mPasswordEditText.setText(account.getPassword());
        mPasswordEditText.setSelection(mPasswordEditText.getText().length());

        mPasswordEditText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                handleLogin();
                return true;
            }
        });

        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });
        mGuestButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleGuestLogin();
            }
        });
    }

    private void handleLogin() {
        // reset errors
        mUserEditText.setError(null);
        mPasswordEditText.setError(null);

        String user = mUserEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();

        if (user.isEmpty()) {
            mUserEditText.setError(getString(R.string.error_user_required));
            mUserEditText.requestFocus();
            return;
        }

        attemptLogin(new FundusAccount(user, password));
    }

    private void handleGuestLogin() {
        mUserEditText.setText(mGuestAccount.getUser());
        mPasswordEditText.setText(mGuestAccount.getPassword());
        attemptLogin(mGuestAccount);
    }

    private void attemptLogin(final FundusAccount account) {
        final FundusApplication app = (FundusApplication) getApplication();
        final FundusAccount oldAccount = app.getAccount();
        app.setAccount(account);
        ServiceConnection serviceConnection = app.getServiceConnection();

        serviceConnection.checkService(new Callback<Response>() {

            @Override
            public void success(Response response1, Response response2) {
                onAuthorized();
            }

            @Override
            public void failure(RetrofitError error) {
                app.setAccount(oldAccount);
                Response response = error.getResponse();
                if (response != null && response.getStatus() == 401) {
                    showNotAuthorized();
                } else {
                    showServiceUnavailable();
                }
            }
        });
    }

    private void onAuthorized() {
        Intent intent = new Intent(this, ItemSelectActivity.class);
        // reuse an existing ItemSelectActivity, when repeatedly pressing the logout and back button
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showServiceUnavailable() {
        Toast.makeText(this, R.string.service_error, Toast.LENGTH_SHORT).show();
    }

    private void showNotAuthorized() {
        mPasswordEditText.setError(getString(R.string.error_not_authorized));
        mPasswordEditText.requestFocus();
    }

}