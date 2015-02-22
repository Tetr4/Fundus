package de.hundebarf.bestandspruefer;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import de.hundebarf.bestandspruefer.collection.FundusAccount;

public class FundusApplication extends Application {
	private static final String PREFERENCES = "FUNDUS_PREFERENCES";
	private static final String LAST_KNOWN_URL_PREFERENCE = "LAST_KNOWN_URL_PREFERENCE";
	private static final String USER_PREFERENCE = "USER_PREFERENCE";
	private static final String PASSWORD_PREFERENCE = "PASSWORD_PREFERENCE";
	private SharedPreferences mPreferences;
	
	public static final String SERVICE_URI = "bestand";
	public static final String[] VALID_SSIDS = new String[] { "HundeBARF",	"BatCave" };

	private FundusAccount mAccount;
	private FundusAccount mGuestAccount = new FundusAccount("Fundus", "1234");
	
	@Override
	public void onCreate() {
		mPreferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
		String user = mPreferences.getString(USER_PREFERENCE, null);
		String password = mPreferences.getString(PASSWORD_PREFERENCE, null);
		if(user != null && password != null) {
			mAccount = new FundusAccount(user, password);
		}
		super.onCreate();
	}
	
	public FundusAccount getAccount() {
		if(mAccount == null) {
			return mGuestAccount;
		}
		return mAccount;
	}

	public void setAccount(FundusAccount account) {
		Editor editor = mPreferences.edit();
		if(account != null) {
			editor.putString(USER_PREFERENCE, account.getUser());
			editor.putString(PASSWORD_PREFERENCE, account.getPassword());
		} else {
			editor.remove(USER_PREFERENCE);
			editor.remove(PASSWORD_PREFERENCE);
		}
		editor.commit();
		mAccount = account;
	}
	
	public String getLastKnownServiceURL() {
		return mPreferences.getString(FundusApplication.LAST_KNOWN_URL_PREFERENCE, null);
	}
	
	public void SetLastKnownServiceURL(String lastKnownServiceURL) {
		mPreferences.edit().putString(LAST_KNOWN_URL_PREFERENCE, lastKnownServiceURL);
	}
	
}
