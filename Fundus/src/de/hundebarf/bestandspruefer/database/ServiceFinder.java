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

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import de.hundebarf.bestandspruefer.FundusApplication;

public class ServiceFinder {
	private static final String TAG = ServiceFinder.class.getSimpleName();
	private Context mContext;
	private CredentialsProvider mCredsProvider;

	public ServiceFinder(Context context, BasicCredentialsProvider credsProvider) {
		mContext = context;
		mCredsProvider = credsProvider;
	}

	public String getServiceURL() throws DatabaseException {
		if (!isWifiEnabled()) {
			// connection to service requires local network
			Log.i(TAG, "Wifi not connected"); 
			throw new DatabaseException("Wifi not connected");
		}

		String ssid = getSSID();
		List<String> validSSIDS = Arrays.asList(FundusApplication.VALID_SSIDS);
		if (!validSSIDS.contains(ssid)) {
			Log.i(TAG, "Wifi SSID (" + ssid + ") is not valid");
			throw new DatabaseException("Wifi SSID not valid");
		}

		// check if last known url is valid
		// last known service url
		FundusApplication app = (FundusApplication) mContext.getApplicationContext();
		String lastKnownServiceURL = app.getLastKnownServiceURL();
		if (lastKnownServiceURL != null) {
			CheckConnection checkConnection = new CheckConnection(lastKnownServiceURL, mCredsProvider);
			CheckConnectionResult result = checkConnection.call();
			if (result != null) {
				// found a http server
				if(result.statuscode == 200) { // OK
					Log.i(TAG, "Last known Service URL is valid.");
					return lastKnownServiceURL;
				} else if (result.statuscode == 401) { // Not Authorized
					throw new DatabaseException("Not authorized", result.statuscode);
				} else {
					// unknown Server or service problem
				}
			}
		}

		// TODO Network Service Discovery?

		// find service by parallel requests to ip range
		// e.g. "192.168.0.0" to "192.168.0.254"

		String ipAddress = getLocalIp();
		ipAddress = ipAddress.substring(0, ipAddress.lastIndexOf("."));
		ipAddress += ".";

		// executor for parallel requests
		int range = 255;
		int nrThreads = range; // FIXME 255 threads maybe too much
		ExecutorService executor = Executors.newFixedThreadPool(nrThreads);
		CompletionService<CheckConnectionResult> complService = new ExecutorCompletionService<CheckConnectionResult>(
				executor);
		List<Future<CheckConnectionResult>> futures = new ArrayList<Future<CheckConnectionResult>>(range);

		// free memory for threads
		System.runFinalization();
		System.gc();

		// create callables
		for (int i = 0; i < range; i++) {
			String curURL = "http://" + ipAddress + i + "/" + FundusApplication.SERVICE_URI + "/";
			CheckConnection checkConnection = new CheckConnection(curURL, mCredsProvider);
			futures.add(complService.submit(checkConnection));
		}

		// get results in completion order
		for (int i = 0; i < range; i++) {
			CheckConnectionResult possibleResult = null;
			try {
				possibleResult = complService.take().get();
			} catch (ExecutionException e) {
				// Ignore
				continue;
			} catch (InterruptedException e) {
				// Ignore
				continue;
			}
			if (possibleResult != null && possibleResult.statuscode == 200) {
				// service found -> cancel other futures
				for (Future<CheckConnectionResult> curFuture : futures) {
					if (!curFuture.isDone() && !curFuture.isCancelled()) {
						curFuture.cancel(false);
					}
				}
				lastKnownServiceURL = possibleResult.url;
				app.SetLastKnownServiceURL(lastKnownServiceURL);
				Log.i(TAG, "Found WebService at: " + lastKnownServiceURL);
				return lastKnownServiceURL;
			}
		}

		Log.i(TAG, "Could not find Webservice");
		throw new DatabaseException("Could not find Webservice", 404);
	}

	private boolean isWifiEnabled() {
		ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifi.isConnected();
	}

	private String getSSID() {
		WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		String ssid = wifiInfo.getSSID();
		// remove quotation marks
		return ssid.replace("\"", "");
	}

	@SuppressLint("DefaultLocale")
	private String getLocalIp() {
		WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		int ip = wifiManager.getConnectionInfo().getIpAddress();
		return String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
	}

	private static void finishResponse(HttpResponse response) {
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

	private static class CheckConnection implements Callable<CheckConnectionResult> {
		private String _url;
		private CredentialsProvider _credsProvider;
		private static final int TIMEOUT = 7000;
	
		public CheckConnection(String url, CredentialsProvider credsProvider) {
			_url = url;
			_credsProvider = credsProvider;
		}
	
		@Override
		public CheckConnectionResult call() {
			HttpHead head = new HttpHead(_url);
			HttpResponse response = null;
	
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT);
			DefaultHttpClient client = new DefaultHttpClient(httpParams);
			client.setCredentialsProvider(_credsProvider);
	
			try {
				response = client.execute(head);
				int statuscode = response.getStatusLine().getStatusCode();
				return new CheckConnectionResult(_url, statuscode);
			} catch (IOException e) {
				// Not a valid Service URL
			} finally {
				finishResponse(response);
			}
			return null;
		}
	}

	private static class CheckConnectionResult {
		CheckConnectionResult(String url, int statuscode) {
			this.url = url;
			this.statuscode = statuscode;
		}
	
		String url;
		int statuscode;
	}

}
