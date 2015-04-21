package de.hundebarf.fundus;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import retrofit.RetrofitError;
import retrofit.RetrofitError.Kind;

/**
 * Abstract Activity which checks service availability and validates user authorization.
 * Starts LoginActivity when not authorized.
 */
public abstract class BaseActivity extends Activity {

    protected static final String TAG = BaseActivity.class.getSimpleName();

    private MenuItem mRefreshMenuItem;
    private boolean mRefreshing;

    /**
     * Called when the service is unavailable
     *
     * @param error the error causing the unavailability
     */
    protected final void handleServiceError(RetrofitError error) {
        doneRefreshing();
        if (error.getKind() == Kind.HTTP) {
            switch (error.getResponse().getStatus()) {
                // react to different http error codes
                case 401:
                    onNotAuthorized();
                    return;
            }
        }
        // TODO more Info
        Log.w(TAG, "Service error", error);
        Toast.makeText(BaseActivity.this, getString(R.string.service_error), Toast.LENGTH_LONG).show();
    }

    protected final void handleServiceSuccess() {
        doneRefreshing();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mRefreshMenuItem = menu.findItem(R.id.action_refresh);
        if(mRefreshing && mRefreshMenuItem != null) {
            mRefreshMenuItem.setActionView(R.layout.action_progressbar);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            refresh();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected final void refresh() {
        mRefreshing = true;
        if(mRefreshMenuItem != null) {
            mRefreshMenuItem.setActionView(R.layout.action_progressbar);
        }
        onRefresh();
    }

    private void doneRefreshing() {
        mRefreshing = false;
        if(mRefreshMenuItem != null) {
            mRefreshMenuItem.collapseActionView();
            mRefreshMenuItem.setActionView(null);
        }
    }

    protected abstract void onRefresh();
}
