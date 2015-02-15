package de.hundebarf.bestandspruefer.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import de.hundebarf.bestandspruefer.FundusApplication;
import android.annotation.SuppressLint;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class ServiceFinder {
	private static final String TAG = ServiceFinder.class.getSimpleName();
	private String mLastKnownServiceURL;
	private SharedPreferences mPreferences;
	private Context mContext;
	private DefaultHttpClient mClient;
	private CredentialsProvider mCredsProvider;
	
	public ServiceFinder(Context context, DefaultHttpClient client, BasicCredentialsProvider credsProvider) {
		mContext = context;
		mClient = client;
		mCredsProvider = credsProvider;
		
		// last known service url
		mPreferences = context.getSharedPreferences(FundusApplication.PREFERENCES, Context.MODE_PRIVATE);
		mLastKnownServiceURL = mPreferences.getString(FundusApplication.LAST_KNOWN_URL_PREFERENCE, null);
	}

	@SuppressLint("DefaultLocale")
	public String getServiceURL() throws DatabaseException {
		// check if wifi is enabled
		ConnectivityManager connManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifi = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (!wifi.isConnected()) {
			// connection to service requires local network
			Log.i(TAG, "Wifi not connected");
			throw new DatabaseException("Wifi not connected");
		}

		// check if wifi ssid is valid
		WifiManager wifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		String ssid = wifiInfo.getSSID();
		ssid = ssid.replace("\"", ""); // remove quot. marks
		List<String> validSSIDS = Arrays.asList(FundusApplication.VALID_SSIDS);
		if (!validSSIDS.contains(ssid)) {
			Log.i(TAG, "Wifi SSID (" + ssid + ") is not valid");
			throw new DatabaseException("Wifi SSID not valid");
		}

		// check if last known url is valid
		if (mLastKnownServiceURL != null) {
			HttpHead head = new HttpHead(mLastKnownServiceURL);
			HttpResponse response = null;
			int statuscode = 0;
			try {
				response = mClient.execute(head);
				statuscode = response.getStatusLine().getStatusCode();
			} catch (IOException e) {
				// Not a valid Service URL -> continue
			} finally {
				finishResponse(response);
			}
			if (statuscode == 200) { // OK
				Log.i(TAG, "Last known Service URL is valid.");
				return mLastKnownServiceURL;
			} else if (statuscode == 401) { // Not Authorized
				throw new DatabaseException("Not authorized", statuscode);
			}
		}

		// TODO Network Service Discovery?

		// find service by parallel requests to ip range
		// e.g. "192.168.0.0" to "192.168.0.254"
		
		// get local ip
		int ipInt = wifiManager.getConnectionInfo().getIpAddress();
		String ipAddress = String.format("%d.%d.%d.%d",
				(ipInt & 0xff), (ipInt >> 8 & 0xff), (ipInt >> 16 & 0xff), (ipInt >> 24 & 0xff));
		Log.i(TAG, "IP: " + ipAddress);
		ipAddress = ipAddress.substring(0, ipAddress.lastIndexOf("."));
		ipAddress += ".";

		// executor for parallel requests
		// FIXME 255 threads too much
		int range = 255;
		ExecutorService executor = Executors.newFixedThreadPool(range);
		CompletionService<String> complService = new ExecutorCompletionService<String>(executor);
		List<Future<String>> futures = new ArrayList<Future<String>>(range);
		
		// create callables
		// free memory for threads
		System.runFinalization();
		System.gc();
		for (int i = 0; i < range; i++) {
			String curURL = "http://" + ipAddress + i + "/" + FundusApplication.SERVICE_URI + "/";
			CheckConnection checkConnection = new CheckConnection(curURL, mCredsProvider);
			futures.add(complService.submit(checkConnection));
		}

		// get first result
		try {
			for (int i = 0; i < range; i++) {
				String possibleResult = complService.take().get();
				if (possibleResult != null) {
					mLastKnownServiceURL = possibleResult;
					mPreferences
							.edit()
							.putString(
									FundusApplication.LAST_KNOWN_URL_PREFERENCE,
									mLastKnownServiceURL).commit();
					Log.i(TAG, "Found WebService at: " + mLastKnownServiceURL);
					return mLastKnownServiceURL;
				}
			}
		} catch (ExecutionException e) {
			// Ignore
		} catch (InterruptedException e) {
			// Ignore
		} finally {
			for (Future<String> curFuture : futures) {
				if (!curFuture.isDone() && !curFuture.isCancelled()) {
					curFuture.cancel(false);
				}
			}
		}

		Log.i(TAG, "Could not find Webservice");
		throw new DatabaseException("Could not find Webservice", 404);
	}

	private static class CheckConnection implements Callable<String> {
		private String _url;
		private CredentialsProvider _credsProvider;
		private static final int TIMEOUT = 4000;

		public CheckConnection(String url, CredentialsProvider credsProvider) {
			_url = url;
			_credsProvider = credsProvider;
		}

		@Override
		public String call() throws Exception {
			HttpHead head = new HttpHead(_url);
			HttpResponse response = null;

			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT);
			DefaultHttpClient client = new DefaultHttpClient(httpParams);
			client.setCredentialsProvider(_credsProvider);

			try {
				response = client.execute(head);
				int statuscode = response.getStatusLine().getStatusCode();
				if (statuscode == 200) { // OK
					return _url;
				}
			} catch (IOException e) {
				// Not a valid Service URL
			} finally {
				if (response != null) {
					try {
						response.getEntity().consumeContent();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}
	}
	
	private void finishResponse(HttpResponse response) {
		if (response != null) {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e) {
					Log.w(TAG, e);
				}
			}
		}
	}

}
