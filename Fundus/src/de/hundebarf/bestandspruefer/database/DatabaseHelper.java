package de.hundebarf.bestandspruefer.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import de.hundebarf.bestandspruefer.collection.Item;

public class DatabaseHelper {
	private static final String TAG = DatabaseHelper.class.getSimpleName();
	private DefaultHttpClient mClient;
	private String mServiceURL;
	// private String mLastKnownServiceURL = "http://192.168.178.17/bestand/";
	private String mLastKnownServiceURL = "http://192.168.1.118/bestand/";
	private static final String SERVICE_URI = "bestand";
	private static final int CONNECTION_TIMEOUT = 6000;
	private Context mContext;
	private CredentialsProvider mCredsProvider;

	public DatabaseHelper(Context context) {
		this(context, "Fundus", "1111");
	}

	public DatabaseHelper(Context context, String user, String pass) {
		mContext = context;

		mCredsProvider = new BasicCredentialsProvider();
		mCredsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST,
				AuthScope.ANY_PORT),
				new UsernamePasswordCredentials(user, pass));

		// timeout
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams,
				CONNECTION_TIMEOUT);

		mClient = new DefaultHttpClient(httpParams);
		mClient.setCredentialsProvider(mCredsProvider);
	}

	public void setLastKnownServiceIP(String ip) {
		mLastKnownServiceURL = "http://" + ip + "/" + SERVICE_URI + "/";
	}

	public List<Item> queryItemList() throws DatabaseException {
		String serviceURL = getServiceURL();
		if (serviceURL == null) {
			throw new DatabaseException("Webservice not found");
		}

		HttpGet get = new HttpGet(serviceURL);

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

		} catch (Exception e) {
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

	public Item queryItem(int itemId) throws DatabaseException {
		String serviceURL = getServiceURL();
		if (serviceURL == null) {
			throw new DatabaseException("Webservice not found");
		}

		HttpGet get = new HttpGet(serviceURL + itemId);
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

		} catch (Exception e) {
			throw new DatabaseException(e);

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

	public void updateQuantity(int itemId, int quantity)
			throws DatabaseException {
		String serviceURL = getServiceURL();
		if (serviceURL == null) {
			throw new DatabaseException("Webservice not found");
		}

		HttpPut put = new HttpPut(serviceURL + itemId);

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("quantity", Integer
					.toString(quantity)));
			put.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse response = mClient.execute(put);

			int statuscode = response.getStatusLine().getStatusCode();
			switch (statuscode) {
			case 200: // OK
				// do nothing
				break;

			default:
				String statusMessage = response.getStatusLine()
						.getReasonPhrase();
				throw new DatabaseException(statusMessage);
			}

		} catch (IOException e) {
			throw new DatabaseException(e);
		}

	}

	public void addItem(Item item) throws DatabaseException {
		// TODO implement
	}

	private JSONObject parseJsonFromHttpResponse(HttpResponse response)
			throws JSONException, IllegalStateException, IOException {
		InputStream contentStream = response.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				contentStream, "UTF-8"), 8);

		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}
		contentStream.close();
		String jsonString = sb.toString();

		return new JSONObject(jsonString);
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

	private String getServiceURL() {
		if (mContext != null) {
			ConnectivityManager connManager = (ConnectivityManager) mContext
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo wifi = connManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (!wifi.isConnected()) {
				// connection to service requires local network
				Log.i(TAG, "Wifi not connected");
				return null;
			}
		}

		if (mServiceURL != null) {
			return mServiceURL;
		}

		if (mLastKnownServiceURL != null) {
			HttpHead head = new HttpHead(mLastKnownServiceURL);
			try {
				HttpResponse response = mClient.execute(head);
				int statuscode = response.getStatusLine().getStatusCode();
				if (statuscode == 200) { // OK
					mServiceURL = mLastKnownServiceURL;
					return mServiceURL;
				}
			} catch (IOException e) {
				// Not a valid Service URL
			}
		}

		// FIXME
		String localIp = "192.168.178.7";
		localIp = localIp.substring(0, localIp.lastIndexOf("."));
		localIp += ".";

		// parallel requests to ip range
		// e.g. "192.168.0.0" to "192.168.0.254"
		// TODO 255 threads too much?
		ExecutorService executor = Executors.newFixedThreadPool(255);
		CompletionService<String> complService = new ExecutorCompletionService<String>(
				executor);
		List<Future<String>> futures = new ArrayList<Future<String>>(255);
		try {
			// create callables
			for (int i = 0; i < 255; i++) {
				String curURL = "http://" + localIp + i + "/" + SERVICE_URI;
				CheckConnection checkConnection = new CheckConnection(curURL);
				futures.add(complService.submit(checkConnection));
			}

			// get first result
			for (int i = 0; i < 255; i++) {
				try {
					String possibleResult = complService.take().get();
					if (possibleResult != null) {
						mServiceURL = possibleResult;
						Log.i(TAG, "Found WebService at: " + mServiceURL);
						return mServiceURL;
					}
				} catch (ExecutionException e) {
					// Ignore
				} catch (InterruptedException e) {
					// Ignore
				}
			}
		} finally {
			for (Future<String> curFuture : futures)
				curFuture.cancel(true);
		}

		return null;
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
