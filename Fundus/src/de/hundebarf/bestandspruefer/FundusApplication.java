package de.hundebarf.bestandspruefer;

import android.app.Application;
import android.content.SharedPreferences;

public class FundusApplication extends Application {
	public static final String PREFERENCES = "FUNDUS_PREFERENCES";
	public static final String LAST_KNOWN_URL_PREFERENCE = "LAST_KNOWN_URL_PREFERENCE";
	public static final String SERVICE_URI = "bestand";
	public static final String[] VALID_SSIDS = new String[] { "HundeBARF",	"BatCave" };
	public static final String SKIP_LOGIN_PREFERENCE = "SKIP_LOGIN_PREFERENCE";
	
	private SharedPreferences mPreferences;

	private String mUser;
	private String mPassword;
	
	@Override
	public void onCreate() {
		mPreferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
		super.onCreate();
	}
	
	public boolean isSkipLogin() {
		return mPreferences.getBoolean(SKIP_LOGIN_PREFERENCE, false);
	}
	
	public void setSkipLogin(boolean skipLogin) {
		mPreferences.edit().putBoolean(SKIP_LOGIN_PREFERENCE, skipLogin).commit();
	}

	public String getUser() {
		return mUser;
	}

	public void setUser(String user) {
		mUser = user;
	}

	public String getPassword() {
		return mPassword;
	}

	public void setPassword(String password) {
		mPassword = password;
	}
	
	
}
