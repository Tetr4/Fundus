package de.hundebarf.fundus;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import de.hundebarf.fundus.database.ServiceConnection;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.RetrofitError.Kind;
import retrofit.client.Response;

/**
 * Abstract Activity which checks service availability and validates user authorization.
 * Starts LoginActivity when not authorized.
 */
public abstract class BaseActivity extends Activity {

    protected static final String TAG = BaseActivity.class.getSimpleName();

    @Override
    protected void onStart() {
        super.onStart();
        FundusApplication app = (FundusApplication) getApplication();
        ServiceConnection connection = app.getServiceConnection();
        connection.checkService(new Callback<Response>() {

            @Override
            public void success(Response response, Response r2) {
                onServiceAvailable();
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getKind() == Kind.HTTP) {
                    switch (error.getResponse().getStatus()) {
                        // react to different http error codes
                        case 401:
                            onNotAuthorized();
                            return;
                    }
                }
                onServiceError(error);
            }
        });
    }

    /**
     * Start the {@link LoginActivity}
     */
    private void onNotAuthorized() {
        Toast.makeText(BaseActivity.this, getString(R.string.error_not_authorized), Toast.LENGTH_SHORT).show();
        Intent loginIntent = new Intent(BaseActivity.this, LoginActivity.class);
        // Clear backstack to disable back navigation from LoginActivity
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
    }

    /**
     * Called when the service is unavailable
     *
     * @param error the error causing the unavailability
     */
    protected void onServiceError(RetrofitError error) {
        // TODO more Info
        Log.w(TAG, "Service error", error);
        Toast.makeText(BaseActivity.this, getString(R.string.service_error), Toast.LENGTH_LONG).show();
    }

    /**
     * Called when the service is available and the user is authorized
     */
    protected abstract void onServiceAvailable();
}
