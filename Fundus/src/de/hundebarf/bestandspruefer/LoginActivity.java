package de.hundebarf.bestandspruefer;

import java.util.Set;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import de.hundebarf.bestandspruefer.database.DatabaseConnection;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.ServiceConnection;
import de.hundebarf.bestandspruefer.database.tasks.DatabaseConnectionTask;

public class LoginActivity extends Activity {
	private static final String ACCOUNT_TYPE = "de.hundebarf";
	private Account mAccount;
	private AccountManager mAccountManager;

	private Button mLoginButton;
	private Button mSkipButton;
	private EditText mUserEditText;
	private EditText mPasswordEditText;
	private DatabaseConnectionTask<Boolean> mCheckAuthorizationTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// check if skip login
		FundusApplication app = (FundusApplication) getApplicationContext();
		if (app.isSkipLogin()) {
			startMainActivity();
			return;
		}

		// check if already authorized
		mAccountManager = AccountManager.get(this);
		mAccount = getAccount();
		if (mAccount != null /* && isAuthorized(mAccount) */) {
			startMainActivity();
			return;
		}

		// no account / not authorized -> login GUI
		setContentView(R.layout.activity_login);
		// Title can't be set in manifest, otherwise the launcher name changes
		setTitle(R.string.activity_login_title);

		mLoginButton = (Button) findViewById(R.id.button_login);
		mSkipButton = (Button) findViewById(R.id.button_skip);
		mUserEditText = (EditText) findViewById(R.id.edittext_user);
		mPasswordEditText = (EditText) findViewById(R.id.edittext_password);

		mLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleLogin();
			}
		});

		mSkipButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleSkip();
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
					createAccount(user, password);
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
		mCheckAuthorizationTask.execute(new ServiceConnection(this, user,
				password));
	}

	private void handleSkip() {
		FundusApplication app = (FundusApplication) getApplicationContext();
		app.setSkipLogin(true);
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

	private Account getAccount() {
		Account[] accounts = mAccountManager.getAccountsByType(ACCOUNT_TYPE);
		if (accounts.length > 0) {
			return accounts[0];
		}
		return null;
	}

	private void createAccount(String user, String password) {
		Account account = new Account(user, ACCOUNT_TYPE);
		mAccountManager.setPassword(account, password);
		mAccountManager.addAccountExplicitly(account, password, null);
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
