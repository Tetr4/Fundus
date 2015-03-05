package de.hundebarf.bestandspruefer;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import de.hundebarf.bestandspruefer.database.ServiceConnection;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class BaseActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FundusApplication app = (FundusApplication) getApplication();
		ServiceConnection connection = app.getServiceConnection();
		connection.checkService(new Callback<Response>() {
			
			@Override
			public void success(Response r1, Response r2) {
				// TODO Show connected to Service
			}
			
			@Override
			public void failure(RetrofitError error) {
				int statusCode = error.getResponse().getStatus();
				if(statusCode == 403) { // Unauthorized
					Intent loginIntent = new Intent(BaseActivity.this, LoginActivity.class);
					startActivity(loginIntent);
				} else {
					// TODO Show Service not available
				}
			}
		});
		
	}

}
