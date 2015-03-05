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
	
	// TODO Resource
	private final FundusAccount mGuestAccount = new FundusAccount("Fundus", "1234");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_login);
		
		mLoginButton = (Button) findViewById(R.id.button_login);
		mGuestButton = (Button) findViewById(R.id.button_guest);
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
		
		FundusApplication app = (FundusApplication) getApplication();
		String user = mUserEditText.getText().toString();
		String password = mPasswordEditText.getText().toString();
		
		if (user.isEmpty()) {
			mUserEditText.setError(getString(R.string.error_user_required));
			mUserEditText.requestFocus();
			return;
		}
		
		app.setAccount(new FundusAccount(user, password));
		attemptLogin();
	}
	
	private void handleGuestLogin() {
		FundusApplication app = (FundusApplication) getApplication();
		app.setAccount(mGuestAccount);
		mUserEditText.setText(mGuestAccount.getUser());
		mPasswordEditText.setText(mGuestAccount.getPassword());
		attemptLogin();
	}
	
	private void attemptLogin() {
		FundusApplication app = (FundusApplication) getApplication();
		ServiceConnection serviceConnection = app.getServiceConnection();
		serviceConnection.checkService(new Callback<Response>() {
			
			@Override
			public void success(Response response1, Response response2) {
				onAuthorized();
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

	private void onAuthorized() {
		startActivity(new Intent(this, ItemSelectActivity.class));
	}

	private void showServiceUnavailable() {
		// TODO String resource
		Toast.makeText(this, "Service unavailable", Toast.LENGTH_SHORT).show();

	}

	private void showNotAuthorized() {
		mPasswordEditText.setError(getString(R.string.error_not_authorized));
		mPasswordEditText.requestFocus();
	}

}