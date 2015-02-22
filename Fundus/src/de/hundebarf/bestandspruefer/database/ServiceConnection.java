package de.hundebarf.bestandspruefer.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
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
import android.util.Log;
import de.hundebarf.bestandspruefer.collection.Item;

public class ServiceConnection implements DatabaseConnection {
	private static final String TAG = ServiceConnection.class.getSimpleName();
	private static final int CONNECTION_TIMEOUT = 5000;
	private DefaultHttpClient mClient;
	private ServiceFinder mFinder;

	public ServiceConnection(Context context) {
		// FIXME Login
		this(context, "Fundus", "1234");
	}

	public ServiceConnection(Context context, String user, String pass) {
		// httpclient with timeout
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
		mClient = new DefaultHttpClient(httpParams);

		// / credentials
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST,
				AuthScope.ANY_PORT),
				new UsernamePasswordCredentials(user, pass));
		mClient.setCredentialsProvider(credsProvider);

		mFinder = new ServiceFinder(context, credsProvider);
	}
	
	public boolean checkAuthorization() throws DatabaseException {
		try {
			mFinder.getServiceURL();
		} catch (DatabaseException e) {
			if(e.getStatusCode() == 401) {
				// not authorized
				return false;
			} else {
				// service not available
				throw e;
			}
		}
		// authorized
		return true;
	}

	@Override
	public List<Item> queryItemList() throws DatabaseException {
		HttpGet get = new HttpGet(mFinder.getServiceURL());

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
				throw new DatabaseException(statusMessage, statuscode);
			}

		} catch (IOException e) {
			throw new DatabaseException(e.getMessage());
		} catch (IllegalStateException e) {
			throw new DatabaseException(e.getMessage());
		} catch (JSONException e) {
			throw new DatabaseException(e.getMessage());
		} finally {
			finishResponse(response);
		}
	}

	@Override
	public Item queryItem(int itemId) throws DatabaseException {
		HttpGet get = new HttpGet(mFinder.getServiceURL() + itemId);

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
				throw new DatabaseException(statusMessage, statuscode);
			}

		} catch (IOException e) {
			throw new DatabaseException(e.getMessage());
		} catch (IllegalStateException e) {
			throw new DatabaseException(e.getMessage());
		} catch (JSONException e) {
			throw new DatabaseException(e.getMessage());
		} finally {
			finishResponse(response);
		}
	}

	@Override
	public void updateQuantity(int itemId, int quantity)
			throws DatabaseException {
		HttpPut put = new HttpPut(mFinder.getServiceURL() + itemId);

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
				throw new DatabaseException(statusMessage, statuscode);
			}

		} catch (IOException e) {
			throw new DatabaseException(e.getMessage());
		} catch (IllegalStateException e) {
			throw new DatabaseException(e.getMessage());
		} finally {
			finishResponse(response);
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
		// TODO GSON?
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
