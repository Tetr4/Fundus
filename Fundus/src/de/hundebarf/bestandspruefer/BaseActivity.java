package de.hundebarf.bestandspruefer;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.RetrofitError.Kind;
import retrofit.client.Response;
import de.hundebarf.bestandspruefer.database.ServiceConnection;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public abstract class BaseActivity extends Activity {

	protected static final String TAG = BaseActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FundusApplication app = (FundusApplication) getApplication();
		ServiceConnection connection = app.getServiceConnection();
		connection.checkService(new Callback<Response>() {

			@Override
			public void success(Response response, Response r2) {
				handleSuccess(response);
			}

			@Override
			public void failure(RetrofitError error) {
				handleError(error);
			}
		});

	}

	protected void handleSuccess(Response response) {
		// TODO Better feedback
	}

	protected void handleError(RetrofitError error) {
		// TODO Better feedback
		if (error.getKind() == Kind.HTTP) {
			switch (error.getResponse().getStatus()) {
			case 401: // Unauthorized -> logout
				Intent loginIntent = new Intent(BaseActivity.this, LoginActivity.class);
				loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(loginIntent);
				return;
			}
		}
		Log.w(TAG, "Retrofit error", error);
		Toast.makeText(this, "Service not available", Toast.LENGTH_LONG).show();
	}

}
