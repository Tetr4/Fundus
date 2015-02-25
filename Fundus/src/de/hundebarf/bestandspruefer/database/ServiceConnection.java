package de.hundebarf.bestandspruefer.database;

import java.util.List;

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
import de.hundebarf.bestandspruefer.collection.Item;

public interface ServiceConnection {
	// returns application/json
	@GET("/bestand/")
	public void queryItemList(Callback<List<Item>> cb);
	
	// returns application/json
	@GET("/bestand/{itemId}/") // application/json
	public void queryItem(@Path("itemId") int itemId, Callback<Item> cb);

	// send quantity as application/x-www-form-urlencoded
	@FormUrlEncoded // 
	@PUT("/bestand/{itemId}") 
	public void updateQuantity(@Path("itemId") int itemId, @Field("quantity") int quantity, Callback<Response> cb);
	
	// send body as application/json
	@POST("/bestand/")
	public void addItem(@Body Item item, Callback<Response> cb);
	
	// check authorization or availability
	@HEAD("/bestand/")
	public void checkService(Callback<Response> cb);
}
