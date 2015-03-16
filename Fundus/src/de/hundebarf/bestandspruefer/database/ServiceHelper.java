package de.hundebarf.bestandspruefer.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import de.hundebarf.bestandspruefer.FundusApplication;
import de.hundebarf.bestandspruefer.collection.FundusAccount;

public class ServiceHelper {
	private static final String TAG = ServiceHelper.class.getSimpleName();

	private List<String> mValidSSIDs;
	private FundusApplication mApp;

	private OkHttpClient mClient;

	public ServiceHelper(Context context, List<String> validSSIDs) {
		mApp = (FundusApplication) context;
		mValidSSIDs = validSSIDs;
	}
	
//	public boolean isWifiEnabled() {
//		ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
//		NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//		return wifi.isConnected();
//	}

	public boolean isSSIDValid() {
		String ssid = getSSID();
		return mValidSSIDs.contains(ssid);
	}

	public String getServiceURL() {
		if(/*!isWifiEnabled() ||*/ !isSSIDValid()) {
			Log.i(TAG, "Not in valid Wifi Network");
			return null;
		}
		
		String lastKnownUrl = mApp.getServiceURL();
		if(isServiceUrlValid(lastKnownUrl)) {
			Log.i(TAG, "Last known url is valid");
			return lastKnownUrl;
		}
		
		return discoverServiceURL();
	}

	private String getSSID() {
		WifiManager wifiManager = (WifiManager) mApp.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		String ssid = wifiInfo.getSSID();
		// remove quotation marks
		return ssid.replace("\"", "");
	}
	
	@SuppressLint("DefaultLocale")
	private String getLocalIp() {
		WifiManager wifiManager = (WifiManager) mApp.getSystemService(Context.WIFI_SERVICE);
		int ip = wifiManager.getConnectionInfo().getIpAddress();
		return String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
	}

	private OkHttpClient getClient() {
		if (mClient != null) {
			return mClient;
		}
		mClient = new OkHttpClient();
		
		FundusAccount account = mApp.getAccount();
		final String credentials = Credentials.basic(account.getUser(), account.getPassword());
		
		Interceptor interceptor = new Interceptor() {
			@Override
			public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
		        Request request = chain.request();
		        Request modifiedRequest = request.newBuilder()
		            .removeHeader("User-Agent")
				    .addHeader("User-Agent", "Fundus-App")
				    .addHeader("Accept", "application/json")
				    .addHeader("Authorization", credentials)
		            .build();
		        return chain.proceed(modifiedRequest);
			}
		};
		mClient.interceptors().add(interceptor);
		
		mClient.setReadTimeout(5, TimeUnit.SECONDS);
		mClient.setConnectTimeout(5, TimeUnit.SECONDS);
		return mClient;
	}

	private boolean isServiceUrlValid(String url) {
		String responseURL = new CheckConnection(url, getClient()).call();
		return (responseURL != null);
	}

	private String discoverServiceURL() {
		String ipAddress = getLocalIp();
		ipAddress = ipAddress.substring(0, ipAddress.lastIndexOf("."));
		ipAddress += ".";

		// executor for parallel requests
		int range = 255;
		int nrThreads = range; // FIXME 255 threads maybe too much
		ExecutorService executor = Executors.newFixedThreadPool(nrThreads);
		CompletionService<String> complService = new ExecutorCompletionService<String>(executor);
		List<Future<String>> futures = new ArrayList<Future<String>>(range);
		
		// free memory for threads
		System.runFinalization();
		System.gc();

		// create callables
		for (int i = 0; i < range; i++) {
			String curURL = "http://" + ipAddress + i + "/" + FundusApplication.SERVICE_URI + "/";
			CheckConnection checkConnection = new CheckConnection(curURL, getClient());
			futures.add(complService.submit(checkConnection));
		}
		
		// get results in completion order
		for (int i = 0; i < range; i++) {
			String possibleResult = null;
			try {
				possibleResult = complService.take().get();
			} catch (ExecutionException e) {
				// Ignore
				continue;
			} catch (InterruptedException e) {
				// Ignore
				continue;
			}
			if (possibleResult != null) {
				// service found -> cancel other futures
				for (Future<String> curFuture : futures) {
					if (!curFuture.isDone() && !curFuture.isCancelled()) {
						curFuture.cancel(false);
					}
				}
				Log.i(TAG, "Found WebService at: " + possibleResult);
				return possibleResult;
			}
		}

		Log.i(TAG, "Could not find WebService");
		return null;
	}

	private static class CheckConnection implements Callable<String> {
		private String _url;
		private OkHttpClient _client;
	
		public CheckConnection(String url, OkHttpClient client) {
			_url = url;
			_client = client;
		}
	
		@Override
		public String call() {
		    Request request = new Request.Builder()
		    .head()
	        .url(_url)
	        .build();
		    
			Response response;
			try {
				response = _client.newCall(request).execute();
			} catch (IOException e) {
				return null;
			}
			
			if(response.isSuccessful()) {
				return _url;
			} 
			return null;
		}
	}

}
