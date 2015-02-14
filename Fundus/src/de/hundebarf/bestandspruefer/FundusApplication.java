package de.hundebarf.bestandspruefer;

import android.app.Application;

public class FundusApplication extends Application {
	public static final String PREFERENCES = "FUNDUS_PREFERENCES";
	public static final String LAST_KNOWN_URL_PREFERENCE = "LAST_KNOWN_URL_PREFERENCE";
	public static final String SERVICE_URI = "bestand";
	public static final String[] VALID_SSIDS = new String[] { "HundeBARF",	"BatCave" };
	public static final String SKIP_LOGIN_PREFERENCE = "SKIP_LOGIN_PREFERENCE";
}
