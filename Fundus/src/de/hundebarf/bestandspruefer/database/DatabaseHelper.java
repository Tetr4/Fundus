package de.hundebarf.bestandspruefer.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.hundebarf.bestandspruefer.collection.Item;

public class DatabaseHelper {
	private DefaultHttpClient mClient;

	public DatabaseHelper() {
		this("Fundus", "1111");
	}

	public DatabaseHelper(String user, String pass) {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST,
				AuthScope.ANY_PORT),
				new UsernamePasswordCredentials(user, pass));
		mClient = new DefaultHttpClient();
		mClient.setCredentialsProvider(credsProvider);
	}

	public List<Item> queryItemList() throws DatabaseException {
		HttpGet get = new HttpGet("http://192.168.178.17/bestand/");
	
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

	public Item queryItem(int itemId) throws DatabaseException {
		HttpGet get = new HttpGet("http://192.168.178.17/bestand/" + itemId);
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

	public void updateQuantity(int itemId, int quantity) throws DatabaseException {
		HttpPut put = new HttpPut("http://192.168.178.17/bestand/" + itemId);

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
			throws DatabaseException, JSONException, IllegalStateException,
			IOException {
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
	
		JSONObject json = new JSONObject(jsonString);
		return json;
	}

	private Item parseItemFromJson(JSONObject jsonObject) throws JSONException {
		Item item = new Item();
		
		JSONArray dataArray = jsonObject.getJSONArray("data");
		JSONObject data = dataArray.getJSONObject(0);
		item.id = data.getInt("ID");
		item.name = data.getString("name");
		item.barcode =  data.getString("barcode");
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

	private List<Item> parseItemListFromJson(JSONObject jsonObject) throws JSONException {
		List<Item> items = new LinkedList<Item>();
		JSONArray dataArray = jsonObject.getJSONArray("data");
		
		for(int i = 0 ; i < dataArray.length(); i++) {
			JSONObject curItemData = dataArray.getJSONObject(i);
			Item curItem = new Item();
			curItem.id = curItemData.getInt("id");
			curItem.name = curItemData.getString("name");
			curItem.category = curItemData.getString("warengruppe");
			curItem.barcode =  curItemData.getString("barcode");
			items.add(curItem);
		}
		
		return items;
	}

}
