package de.hundebarf.fundus;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import de.hundebarf.fundus.collection.FundusAccount;
import de.hundebarf.fundus.database.ServiceConnection;
import de.hundebarf.fundus.database.ServiceHelper;
import retrofit.Endpoint;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

public class FundusApplication extends Application {
	private static final String TAG = FundusApplication.class.getSimpleName();

	private SharedPreferences mPreferences;
	private static final String FUNDUS_PREFERENCES = "FUNDUS_PREFERENCES";
	private static final String USER_PREFERENCE = "USER_PREFERENCE";
	private static final String PASSWORD_PREFERENCE = "PASSWORD_PREFERENCE";
	private static final String SERVICE_URL_PREFERENCE = "SERVICE_URL_PREFERENCE";

	private ServiceConnection mServiceConnection;
    private ServiceConnection mCacheConnection;
    private ServiceHelper mServiceHelper;
    private static final long TIMEOUT = 7; // seconds

    private String mServiceURL = "http://192.168.42.202";
    private FundusAccount mAccount;

    @Override
    public void onCreate() {
        mPreferences = getSharedPreferences(FUNDUS_PREFERENCES, MODE_PRIVATE);
		
		// Service URL
		mServiceURL = mPreferences.getString(FundusApplication.SERVICE_URL_PREFERENCE, mServiceURL);
		
		// Valid SSIDs
		String[] validSSIDs = getResources().getStringArray(R.array.valid_ssids);
		mServiceHelper = new ServiceHelper(this, Arrays.asList(validSSIDs));
		
		// Account
		String user = mPreferences.getString(USER_PREFERENCE, "");
		String password = mPreferences.getString(PASSWORD_PREFERENCE, "");
		mAccount = new FundusAccount(user, password);
		super.onCreate();
	}

	public FundusAccount getAccount() {
		return mAccount;
	}

	public void setAccount(FundusAccount account) {
		Editor editor = mPreferences.edit();
		if(account == null) {
			editor.remove(USER_PREFERENCE);
			editor.remove(PASSWORD_PREFERENCE);
			mAccount = new FundusAccount("","");
		} else {
			editor.putString(USER_PREFERENCE, account.getUser());
			editor.putString(PASSWORD_PREFERENCE, account.getPassword());
			mAccount = account;
		}
		editor.apply();
	}

	public String getServiceURL() {
		return mServiceURL;
	}

	public void setServiceURL(String serviceURL) {
		mServiceURL = serviceURL;
		mPreferences.edit().putString(SERVICE_URL_PREFERENCE, serviceURL).apply();
	}

    public ServiceConnection getServiceConnection() {
        if (mServiceConnection == null) {
            // lazy init
            createCacheAndNetworkConnections();
        }
        return mServiceConnection;
    }

    public ServiceConnection getCacheConnection() {
        if (mCacheConnection == null) {
            // lazy init
            createCacheAndNetworkConnections();
        }
        return mCacheConnection;
    }

    private void createCacheAndNetworkConnections() {
        // Client for caching and timeouts
        OkHttpClient okHttpClient = new OkHttpClient();
        try {
			Cache cache = new Cache(getCacheDir(), 10 * 1024 * 1024);
			okHttpClient.setCache(cache);
		} catch (IOException e) {
			Log.w(TAG, "Could not create httpClientCache", e);
		}
        okHttpClient.setReadTimeout(TIMEOUT, TimeUnit.SECONDS);
        okHttpClient.setConnectTimeout(TIMEOUT, TimeUnit.SECONDS);

        // forces cache
        // TODO fill complete cache at some point
        RequestInterceptor cacheInterceptor = new FundusInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                super.intercept(request);
                request.addHeader("Cache-Control", "only-if-cached, max-stale=" + Integer.MAX_VALUE);
            }
        };

        // skips cache and forces full refresh
        RequestInterceptor noCacheInterceptor = new FundusInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                super.intercept(request);
                request.addHeader("Cache-Control", "no-cache");
            }
        };

        // Dynamic endpoint
        Endpoint endpoint = new Endpoint() {
            @Override
            public String getUrl() {
				return mServiceURL;
			}
			@Override
			public String getName() {
				return "Fundus-Service";
			}
		};

		// Build RestAdapter
        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setEndpoint(endpoint)
//				.setLogLevel(RestAdapter.LogLevel.FULL)
				.setClient(new OkClient(okHttpClient))
                .setRequestInterceptor(noCacheInterceptor);
        mServiceConnection = builder.build().create(ServiceConnection.class);

        builder.setRequestInterceptor(cacheInterceptor);
        mCacheConnection = builder.build().create(ServiceConnection.class);
    }

    /**
     * Adds Authorization header (and User-Agent, etc.) to every request.
     */
    private class FundusInterceptor implements RequestInterceptor {

        @Override
        public void intercept(RequestFacade request) {
            request.addHeader("User-Agent", "Fundus-App");
            request.addHeader("Accept", "application/json");

            // basic access authentication
            FundusAccount account = getAccount();
            String credentials = Credentials.basic(account.getUser(), account.getPassword());
            request.addHeader("Authorization", credentials);
        }
    }

}
