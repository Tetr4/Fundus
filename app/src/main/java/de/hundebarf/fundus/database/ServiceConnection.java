package de.hundebarf.fundus.database;

import java.util.List;

import de.hundebarf.fundus.collection.Item;
import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.HEAD;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

public interface ServiceConnection {
    public static final String SERVICE_URI = "/inventory/";

	// returns application/json
	@GET(SERVICE_URI)
	public void queryItemList(Callback<List<Item>> cb);
	
	// returns application/json
	@GET(SERVICE_URI+ "{itemId}/") // application/json
	public void queryItem(@Path("itemId") int itemId, Callback<Item> cb);

	// send quantity as application/x-www-form-urlencoded
	@FormUrlEncoded // 
	@PUT(SERVICE_URI + "{itemId}")
	public void updateQuantity(@Path("itemId") int itemId, @Field("quantity") int quantity, Callback<Response> cb);
	
	// send body as application/json
	@POST(SERVICE_URI)
	public void addItem(@Body Item item, Callback<Response> cb);
	
	// check authorization or availability
	@HEAD(SERVICE_URI)
	public void checkService(Callback<Response> cb);
}
