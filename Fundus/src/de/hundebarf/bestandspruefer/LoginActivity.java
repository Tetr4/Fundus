package de.hundebarf.bestandspruefer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;

public class LoginActivity extends Activity {
	private Account mAccount;
	private static final String ACCOUNT_TYPE = "de.hundebarf";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
//		AccountManager accountManager = AccountManager.get(this);
//		Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
//		if (accounts.length > 0) {
//			mAccount = accounts[0];
//			String name = mAccount.name;
//			String password = accountManager.getPassword(mAccount);
//		} else {
//			// add new account
//			mAccount = new Account("Fundus", ACCOUNT_TYPE);
//			accountManager.addAccountExplicitly(mAccount, "1234", null);
//		}
	}

}
