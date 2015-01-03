package de.hundebarf.bestandspruefer.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import de.hundebarf.bestandspruefer.collection.Item;

public class RemoteConnection implements DatabaseConnection {
	private static final String TAG = RemoteConnection.class.getSimpleName();

	private static final String LAST_KNOWN_URL_PREFERENCE = "LAST_KNOWN_URL_PREFERENCE";
	private static final String SERVICE_URI = "bestand";
	private static final String[] VALID_SSIDS = new String[] { "HundeBARF", "BatCave" };
	private static final int CONNECTION_TIMEOUT = 5000;
	private String mLastKnownServiceURL;

	private DefaultHttpClient mClient;
	private CredentialsProvider mCredsProvider;

	private Context mContext;
	private SharedPreferences mPreferences;

	public RemoteConnection(Context context) {
		this(context, "Fundus", "1234");
	}

	public RemoteConnection(Context context, String user, String pass) {
		mContext = context;

		// httpclient with timeout
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams,
				CONNECTION_TIMEOUT);
		mClient = new DefaultHttpClient(httpParams);

		// / credentials
		mCredsProvider = new BasicCredentialsProvider();
		mCredsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST,
				AuthScope.ANY_PORT),
				new UsernamePasswordCredentials(user, pass));
		mClient.setCredentialsProvider(mCredsProvider);

		// last known service url
		mPreferences = context.getSharedPreferences(LAST_KNOWN_URL_PREFERENCE,
				Context.MODE_PRIVATE);
		mLastKnownServiceURL = mPreferences.getString(
				LAST_KNOWN_URL_PREFERENCE, null);
	}

	@Override
	public List<Item> queryItemList() throws DatabaseException {
		HttpGet get = new HttpGet(getServiceURL());

		HttpResponse response = null;
		try {
			response = mClient.execute(get);
			int statuscode = response.getStatusLine().getStatusCode();
			switch (statuscode) {
			case 200: // OK
				JSONObject json = parseJsonFromHttpResponse(response);
				return parseItemListFromJson(json);
			default:
				String statusMessage = response.getStatusLine()
						.getReasonPhrase();
				throw new DatabaseException(statusMessage);
			}

		} catch (IOException e) {
			throw new DatabaseException(e.getMessage());
		} catch (IllegalStateException e) {
			throw new DatabaseException(e.getMessage());
		} catch (JSONException e) {
			throw new DatabaseException(e.getMessage());
		} finally {
			if (response != null) {
				try {
					response.getEntity().consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public Item queryItem(int itemId) throws DatabaseException {
		HttpGet get = new HttpGet(getServiceURL() + itemId);

		HttpResponse response = null;
		try {
			response = mClient.execute(get);
			int statuscode = response.getStatusLine().getStatusCode();
			switch (statuscode) {
			case 200: // OK
				JSONObject json = parseJsonFromHttpResponse(response);
				return parseItemFromJson(json);

			default:
				String statusMessage = response.getStatusLine()
						.getReasonPhrase();
				throw new DatabaseException(statusMessage);
			}

		} catch (IOException e) {
			throw new DatabaseException(e.getMessage());
		} catch (IllegalStateException e) {
			throw new DatabaseException(e.getMessage());
		} catch (JSONException e) {
			throw new DatabaseException(e.getMessage());
		} finally {
			if (response != null) {
				try {
					response.getEntity().consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void updateQuantity(int itemId, int quantity)
			throws DatabaseException {
		HttpPut put = new HttpPut(getServiceURL() + itemId);
		
		// add quantity as content
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("quantity", Integer
				.toString(quantity)));

		HttpResponse response = null;
		try {
			put.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			response = mClient.execute(put);
			int statuscode = response.getStatusLine().getStatusCode();
			switch (statuscode) {
			case 200: // OK
				return;
			default:
				String statusMessage = response.getStatusLine()
						.getReasonPhrase();
				throw new DatabaseException(statusMessage);
			}

		} catch (IOException e) {
			throw new DatabaseException(e.getMessage());
		} catch (IllegalStateException e) {
			throw new DatabaseException(e.getMessage());
		} finally {
			if (response != null) {
				try {
					response.getEntity().consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void addItem(Item item) throws DatabaseException {
		// TODO implement
	}

	private JSONObject parseJsonFromHttpResponse(HttpResponse response)
			throws JSONException, IllegalStateException, IOException {
		InputStream contentStream = response.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				contentStream, "UTF-8"), 8);
		try {
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			String jsonString = sb.toString();
			return new JSONObject(jsonString);
		} finally {
			reader.close();
		}
	}

	private Item parseItemFromJson(JSONObject jsonObject) throws JSONException {
		Item item = new Item();

		JSONArray dataArray = jsonObject.getJSONArray("data");
		JSONObject data = dataArray.getJSONObject(0);
		item.id = data.getInt("ID");
		item.name = data.getString("name");
		item.barcode = data.getString("barcode");
		item.category = data.getString("warengruppe");
		item.stock = data.getInt("menge");
		item.buyingPrice = data.getString("einkaufspreis");
		item.description = data.getString("beschreibung");
		item.price = data.getInt("verkaufspreis");
		item.quanityPackage = data.getInt("menge(verpackung)");
		item.unitPackage = data.getString("einheit(verpackung)");
		item.quantityContent = data.getInt("menge(inhalt)");
		item.unitContent = data.getString("einheit(inhalt)");
		item.shortName = data.getString("kurzname");
		item.size = data.getString("groeße");
		item.supplier = data.getString("lieferant");
		item.taxGroup = data.getString("steuergruppe");

		return item;
	}

	private List<Item> parseItemListFromJson(JSONObject jsonObject)
			throws JSONException {
		List<Item> items = new LinkedList<Item>();
		JSONArray dataArray = jsonObject.getJSONArray("data");

		for (int i = 0; i < dataArray.length(); i++) {
			JSONObject curItemData = dataArray.getJSONObject(i);
			Item curItem = new Item();
			curItem.id = curItemData.getInt("id");
			curItem.name = curItemData.getString("name");
			curItem.category = curItemData.getString("warengruppe");
			curItem.barcode = curItemData.getString("barcode");
			items.add(curItem);
		}

		return items;
	}

	@SuppressLint("DefaultLocale")
	private String getServiceURL() throws DatabaseException {
		// check wifi
		ConnectivityManager connManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifi = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (!wifi.isConnected()) {
			// connection to service requires local network
			Log.i(TAG, "Wifi not connected");
			throw new DatabaseException("Wifi not connected");
		} 
		
		//check wifi ssid
		WifiManager wifiManager= (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		String ssid = wifiInfo.getSSID();
		ssid = ssid.replace("\"", ""); // remove quot. marks
		if(!Arrays.asList(VALID_SSIDS).contains(ssid)) {
			Log.i(TAG, "Wifi SSID (" + ssid + ") is not valid");
			throw new DatabaseException("Wifi SSID not valid");
		}

		// check last known url
		if (mLastKnownServiceURL != null) {
			HttpHead head = new HttpHead(mLastKnownServiceURL);
			try {
				HttpResponse response = mClient.execute(head);
				int statuscode = response.getStatusLine().getStatusCode();
				if (statuscode == 200) { // OK
					Log.i(TAG, "Last known Service URL is valid.");
					return mLastKnownServiceURL;
				}
			} catch (IOException e) {
				// Not a valid Service URL -> continue
			}
		}
		
		// TODO Network Service Discovery?
		// mLastKnownServiceURL = possibleResult;
		// mPreferences.edit().putString(LAST_KNOWN_URL_PREFERENCE,
		// mLastKnownServiceURL).commit();

		// get local ip adress
		String ipAddress = "192.168.178.";
		int ipInt = wifiManager.getConnectionInfo().getIpAddress();
		ipAddress = String
				.format("%d.%d.%d.%d", (ipInt & 0xff), (ipInt >> 8 & 0xff),
						(ipInt >> 16 & 0xff), (ipInt >> 24 & 0xff));
		Log.i(TAG, "IP: " + ipAddress);
		ipAddress = ipAddress.substring(0, ipAddress.lastIndexOf("."));
		ipAddress += ".";

		// parallel requests to ip range
		// e.g. "192.168.0.0" to "192.168.0.254"
		int range = 255;
		// TODO 255 threads too much. maybe broadcast
		ExecutorService executor = Executors.newFixedThreadPool(range);
		CompletionService<String> complService = new ExecutorCompletionService<String>(
				executor);
		List<Future<String>> futures = new ArrayList<Future<String>>(range);
		// free memory for threads
		System.gc();
		System.runFinalization();
		System.gc();
		// create callables
		for (int i = 0; i < range; i++) {
			String curURL = "http://" + ipAddress + i + "/" + SERVICE_URI
					+ "/";
			CheckConnection checkConnection = new CheckConnection(curURL);
			futures.add(complService.submit(checkConnection));
		}
		// get first result
		for (int i = 0; i < range; i++) {
			try {
				String possibleResult = complService.take().get();
				if (possibleResult != null) {
					mLastKnownServiceURL = possibleResult;
					mPreferences
							.edit()
							.putString(LAST_KNOWN_URL_PREFERENCE,
									mLastKnownServiceURL).commit();
					Log.i(TAG, "Found WebService at: "
							+ mLastKnownServiceURL);
					return mLastKnownServiceURL;
				}
			} catch (ExecutionException e) {
				// Ignore
			} catch (InterruptedException e) {
				// Ignore
			} finally {
				for (Future<String> curFuture : futures) {
					curFuture.cancel(true);
				}
			}
		}

		throw new DatabaseException("Could not find Webservice");
	}

	private class CheckConnection implements Callable<String> {
		private String _url;

		public CheckConnection(String url) {
			_url = url;

		}

		@Override
		public String call() throws Exception {
			HttpHead head = new HttpHead(_url);
			try {
				final HttpParams httpParams = new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout(httpParams,
						CONNECTION_TIMEOUT);
				DefaultHttpClient client = new DefaultHttpClient(httpParams);
				client.setCredentialsProvider(mCredsProvider);
				HttpResponse response = client.execute(head);
				int statuscode = response.getStatusLine().getStatusCode();
				if (statuscode == 200) { // OK
					return _url;
				}
			} catch (IOException e) {
				// Not a valid Service URL
			}
			return null;
		}
	}
}
