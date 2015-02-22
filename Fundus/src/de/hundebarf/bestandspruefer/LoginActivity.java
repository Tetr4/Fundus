package de.hundebarf.bestandspruefer;

import java.util.Set;

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
import de.hundebarf.bestandspruefer.database.DatabaseConnection;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.ServiceConnection;
import de.hundebarf.bestandspruefer.database.tasks.DatabaseConnectionTask;

public class LoginActivity extends Activity {
	public static final String SWITCH_ACCOUNT = "SWITCH_ACCOUNT";

	private Button mLoginButton;
	private Button mGuestButton;
	private EditText mUserEditText;
	private EditText mPasswordEditText;
	private DatabaseConnectionTask<Boolean> mCheckAuthorizationTask;

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
			FundusAccount account = getAccount();
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
		final String user = mUserEditText.getText().toString();
		final String password = mPasswordEditText.getText().toString();

		mCheckAuthorizationTask = new DatabaseConnectionTask<Boolean>(this) {
			@Override
			protected Boolean executeQuery(DatabaseConnection con)
					throws DatabaseException {
				return ((ServiceConnection) con).checkAuthorization();
			}

			@Override
			protected void onSuccess(Boolean authorized, DatabaseConnection con) {
				if (authorized) {
					useAccount(new FundusAccount(user, password));
					startMainActivity();
				} else {
					showNotAuthorized();
				}
			}

			@Override
			protected void onFailure(DatabaseException e, DatabaseConnection con) {
				showServiceUnavailable();
			}

			@Override
			protected void onFinished(
					Set<DatabaseConnection> successfulConnections) {
			}
		};
		mCheckAuthorizationTask.execute(new ServiceConnection(this, user, password));
	}

	private void handleGuestLogin() {
		FundusApplication app = (FundusApplication) getApplication();
		app.setAccount(null);
		startMainActivity();
	}
	
	private void useAccount(FundusAccount account) {
//		AccountManager accountManager = AccountManager.get(this);
//		Account account = new Account(user, ACCOUNT_TYPE);
//		accountManager.addAccountExplicitly(account, password, null);
		
		FundusApplication app = (FundusApplication) getApplication();
		app.setAccount(account);
	}

	private FundusAccount getAccount() {
//		AccountManager accountManager = AccountManager.get(this);
//		Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
//		if (accounts.length > 0) {
//			return accounts[0];
//		}
		FundusApplication app = (FundusApplication) getApplication();
		return app.getAccount();
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

	@Override
	protected void onPause() {
		if (mCheckAuthorizationTask != null) {
			mCheckAuthorizationTask.onPause();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		if (mCheckAuthorizationTask != null) {
			mCheckAuthorizationTask.onResume();
		}
		super.onResume();
	}

}
