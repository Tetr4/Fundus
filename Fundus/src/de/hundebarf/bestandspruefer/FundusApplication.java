package de.hundebarf.bestandspruefer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import retrofit.Endpoint;
import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Base64;
import android.util.Log;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;

import de.hundebarf.bestandspruefer.collection.FundusAccount;
import de.hundebarf.bestandspruefer.database.ServiceConnection;

public class FundusApplication extends Application {
	private static final String TAG = FundusApplication.class.getSimpleName();

	private static final String PREFERENCES = "FUNDUS_PREFERENCES";
	private static final String USER_PREFERENCE = "USER_PREFERENCE";
	private static final String PASSWORD_PREFERENCE = "PASSWORD_PREFERENCE";
	private static final String LAST_KNOWN_URL_PREFERENCE = "LAST_KNOWN_URL_PREFERENCE";
	private SharedPreferences mPreferences;

	public static final String SERVICE_URI = "bestand";
	private ServiceConnection mServiceConnection;
	private String mLastKnowUrl = "http://192.168.178.9";
	private Endpoint mEndpoint;

	private FundusAccount mAccount = null;
	private FundusAccount mGuestAccount = new FundusAccount("Fundus", "1234");

	@Override
	public void onCreate() {
		mPreferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
//		mPreferences.edit()
//				.remove(LAST_KNOWN_URL_PREFERENCE)
//				.remove(USER_PREFERENCE)
//				.remove(PASSWORD_PREFERENCE)
//				.commit();
		mLastKnowUrl = mPreferences.getString(FundusApplication.LAST_KNOWN_URL_PREFERENCE, mLastKnowUrl);
		
		String user = mPreferences.getString(USER_PREFERENCE, null);
		String password = mPreferences.getString(PASSWORD_PREFERENCE, null);
		if (user != null && password != null) {
			mAccount = new FundusAccount(user, password);
		}
		super.onCreate();
	}

	public FundusAccount getAccount() {
		if (mAccount == null) {
			return mGuestAccount;
		}
		return mAccount;
	}

	// accepts null to remove account
	public void setAccount(FundusAccount account) {
		Editor editor = mPreferences.edit();
		if (account != null) {
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
		return mLastKnowUrl;
	}

	public void setLastKnownServiceURL(String lastKnownServiceURL) {
		mLastKnowUrl = lastKnownServiceURL;
		mPreferences.edit().putString(LAST_KNOWN_URL_PREFERENCE, lastKnownServiceURL);
	}
	
	public List<String> getValidSSIDs() {
		String[] validSSIDs = getResources().getStringArray(R.array.valid_ssids);
		return Arrays.asList(validSSIDs);
	}

	public ServiceConnection getServiceConnection() {
		if (mServiceConnection == null) {
			// lazy init
			mServiceConnection = createServiceConnection();
		}
		return mServiceConnection;
	}

	private ServiceConnection createServiceConnection() {
		// Caching
		OkHttpClient okHttpClient = new OkHttpClient();
		File cacheDir = getCacheDir();
		Cache cache = null;
		try {
			new Cache(cacheDir, 10 * 1024 * 1024);
		} catch (IOException e) {
			Log.w(TAG, "Could not create httpClientCache cache", e);
		}
		okHttpClient.setCache(cache);

		// Default Header (Authorization, User-Agent, etc.)
		RequestInterceptor interceptor = new RequestInterceptor() {
			@Override
			public void intercept(RequestFacade request) {
				FundusAccount account = getAccount();
				if (account != null) {
					// basic access authentication
					String credentials = account.getUser() + ":" + account.getPassword();
					String credsString = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
					request.addHeader("Authorization", credsString);
				}
				request.addHeader("User-Agent", "Fundus-App");
				request.addHeader("Accept", "application/json");
			}
		};
		
		// Error Handler
		ErrorHandler errorHandler = new ErrorHandler() {
			@Override
			public Throwable handleError(RetrofitError cause) {
				Response response = cause.getResponse();
				if (response != null && response.getStatus() == 401) {
					// 401 Unauthorized -> logout
					setAccount(null);
				}
				return cause;
			}
		};
		
		// Endpoint
		mEndpoint = new Endpoint() {
			@Override
			public String getUrl() {
				return mLastKnowUrl;
			}
			@Override
			public String getName() {
				return "Fundus-Service";
			}
		};

		// Build RestAdapter
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setEndpoint(mEndpoint)
				.setLogLevel(RestAdapter.LogLevel.FULL)
				.setClient(new OkClient(okHttpClient))
				.setRequestInterceptor(interceptor)
				.setErrorHandler(errorHandler)	
				.build();
		
		return restAdapter.create(ServiceConnection.class);
	}

}
