package de.hundebarf.fundus;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.hundebarf.fundus.collection.FundusAccount;
import de.hundebarf.fundus.database.ServiceConnection;
import de.hundebarf.fundus.database.ServiceHelper;
import retrofit.Endpoint;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.android.MainThreadExecutor;
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

    private String mServiceURL = "http://192.168.178.8";
    private FundusAccount mAccount;

    @Override
    public void onCreate() {
        mPreferences = getSharedPreferences(FUNDUS_PREFERENCES, MODE_PRIVATE);

        //Last known Service URL
        mServiceURL = mPreferences.getString(FundusApplication.SERVICE_URL_PREFERENCE, mServiceURL);
        Log.d(TAG, "Last Service URL: " + mServiceURL);

        // Valid SSIDs
        String[] validSSIDs = getResources().getStringArray(R.array.valid_ssids);
        mServiceHelper = new ServiceHelper(this, Arrays.asList(validSSIDs));

        createCacheAndNetworkConnections();

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
        if (account == null) {
            editor.remove(USER_PREFERENCE);
            editor.remove(PASSWORD_PREFERENCE);
            mAccount = new FundusAccount("", "");
        } else {
            editor.putString(USER_PREFERENCE, account.getUser());
            editor.putString(PASSWORD_PREFERENCE, account.getPassword());
            mAccount = account;
        }
        editor.apply();
    }

    public ServiceConnection getServiceConnection() {
        return mServiceConnection;
    }

    public ServiceConnection getCacheConnection() {
        return mCacheConnection;
    }

    private void createCacheAndNetworkConnections() {
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setReadTimeout(TIMEOUT, TimeUnit.SECONDS);
        okHttpClient.setConnectTimeout(TIMEOUT, TimeUnit.SECONDS);
        try {
            Cache cache = new Cache(getCacheDir(), 10 * 1024 * 1024);
            okHttpClient.setCache(cache);
        } catch (IOException e) {
            Log.w(TAG, "Could not create httpClientCache", e);
        }

        okHttpClient.interceptors().add(new CustomInterceptor());

        // forces cache. used for cache connection
        // TODO fill complete cache at some point
        RequestInterceptor cacheInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Cache-Control", "only-if-cached, max-stale=" + Integer.MAX_VALUE);
            }
        };

        // skips cache and forces full refresh. used for service connection
        RequestInterceptor noCacheInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
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
                .setClient(new OkClient(okHttpClient))
                .setRequestInterceptor(noCacheInterceptor)
                        // process requests sequentially
                .setExecutors(Executors.newSingleThreadExecutor(), new MainThreadExecutor());
        mServiceConnection = builder.build().create(ServiceConnection.class);

        builder.setRequestInterceptor(cacheInterceptor);
        mCacheConnection = builder.build().create(ServiceConnection.class);
    }

    /**
     * Intercept each request and add headers, prevent network requests when not in valid network
     * and retry a request with the correct service url on ConnectException
     */
    private class CustomInterceptor implements Interceptor {
        private static final String TAG = "Interceptor";

        @Override
        public Response intercept(Chain chain) throws IOException {

            // add basic access authentication and other common headers to each request
            FundusAccount account = getAccount();
            String credentials = Credentials.basic(account.getUser(), account.getPassword());
            Request authorizedRequest = chain.request().newBuilder()
                    .header("User-Agent", "Fundus-App")
                    .header("Accept", "application/json")
                    .header("Authorization", credentials)
                    .build();

            boolean useCache = authorizedRequest.cacheControl().onlyIfCached();
            if (useCache) {
                // everything okay, no need to check valid network etc.
                return chain.proceed(authorizedRequest);
            }

            if (!mServiceHelper.inValidNetwork()) {
                Log.v(TAG, "Not in valid network");
                // abort request when not in valid wifi network
                return null;
            }

            Response response;
            try {
                response = chain.proceed(authorizedRequest);
            } catch (ConnectException e) {
                // not a valid service url -> search service url and retry
                String foundUrl = mServiceHelper.findServiceURL(mServiceURL);
                if (foundUrl == null) {
                    // service not found
                    Log.v(TAG, "Could not find service");
                    throw e;
                }

                // save new url
                mServiceURL = foundUrl;
                mPreferences.edit().putString(SERVICE_URL_PREFERENCE, foundUrl).apply();

                // retry with new url
                Log.v(TAG, "retry");
                URL retryUrl = new URL(mServiceURL + authorizedRequest.url().getPath());
                Request newUrlRequest = authorizedRequest.newBuilder().url(retryUrl).build();
                return chain.proceed(newUrlRequest);
            }

            // service found
            return response;
        }
    }
}
