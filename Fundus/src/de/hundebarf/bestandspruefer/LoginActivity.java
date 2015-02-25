package de.hundebarf.bestandspruefer;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
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
import de.hundebarf.bestandspruefer.collection.FundusAccount;
import de.hundebarf.bestandspruefer.database.ServiceConnection;

public class LoginActivity extends Activity {
	public static final String SWITCH_ACCOUNT = "SWITCH_ACCOUNT";

	private Button mLoginButton;
	private Button mGuestButton;
	private EditText mUserEditText;
	private EditText mPasswordEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Workaround! Title can't be set in manifest, otherwise the launcher name changes
		setTitle(R.string.activity_login_title);
		
		// check if user wants to login as other user -> disable autologin
		boolean switchAccount = false;
		if(getIntent().getExtras() != null) {
			switchAccount = getIntent().getExtras().getBoolean(SWITCH_ACCOUNT);
		}
		if(!switchAccount) {
			// check if already authorized
			FundusApplication app = (FundusApplication) getApplication();
			FundusAccount account = app.getAccount();
			if (account != null /* && isAuthorized(account) */) {
				startMainActivity();
				return;
			}
		}
		
		// no account / not authorized / switch account -> login GUI
		setContentView(R.layout.activity_login);
		
		mLoginButton = (Button) findViewById(R.id.button_login);
		mGuestButton = (Button) findViewById(R.id.button_guest);
		mUserEditText = (EditText) findViewById(R.id.edittext_user);
		mPasswordEditText = (EditText) findViewById(R.id.edittext_password);
		
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
		FundusApplication app = (FundusApplication) getApplication();
		final String user = mUserEditText.getText().toString();
		final String password = mPasswordEditText.getText().toString();
		app.setAccount(new FundusAccount(user, password));
		ServiceConnection serviceConnection = app.getServiceConnection();
		serviceConnection.checkService(new Callback<Response>() {
			
			@Override
			public void success(Response response1, Response response2) {
				startMainActivity();
			}
			
			@Override
			public void failure(RetrofitError error) {
				Response response = error.getResponse();
				if(response != null && response.getStatus() == 401) {
					showNotAuthorized();
				} else {
					showServiceUnavailable();
				}
			}
		});
	}

	private void handleGuestLogin() {
		FundusApplication app = (FundusApplication) getApplication();
		app.setAccount(null);
		startMainActivity();
	}
	
	private void showServiceUnavailable() {
		// TODO String resource
		Toast.makeText(this, "Service unavailable", Toast.LENGTH_SHORT).show();

	}

	private void showNotAuthorized() {
		// TODO String resource
		Toast.makeText(this, "Not authorized", Toast.LENGTH_SHORT).show();
	}

	private void startMainActivity() {
		Intent intent = new Intent(this, ItemSelectActivity.class);
		startActivity(intent);
		finish();
	}

}