package de.hundebarf.bestandspruefer.database.tasks;

import android.content.Context;
import de.hundebarf.bestandspruefer.database.CacheConnection;
import de.hundebarf.bestandspruefer.database.DatabaseConnection;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.RemoteConnection;

public abstract class CachedDatabaseConnectionTask<Result> {
	private DatabaseConnection mCacheConnection;
	private DatabaseConnection mRemoteConnection;
	private DatabaseConnectionTask<Result> mCacheTask;
	private DatabaseConnectionTask<Result> mRemoteTask;
	private STATE mCacheState = STATE.RUNNING;
	private STATE mRemoteState = STATE.RUNNING;

	private enum STATE {
		SUCCESS, FAILURE, RUNNING
	}

	public CachedDatabaseConnectionTask(Context context) {
		// FIXME possibly 2 progressdialogs
		// cache task
		mCacheTask = new DatabaseConnectionTask<Result>(context) {
			@Override
			protected Result executeQuery(DatabaseConnection connection)
					throws DatabaseException {
				return CachedDatabaseConnectionTask.this
						.executeQuery(connection);
			}

			@Override
			protected void onSuccess(Result result) {
				mCacheState = STATE.SUCCESS;
				switch (mRemoteState) {
				case SUCCESS:
					// should never happen
					break;
				case FAILURE:
					onDataReceived(result, true);
					onFinishedReceiving();
					break;
				case RUNNING:
					onDataReceived(result, true);
					break;
				}
			}

			@Override
			protected void onFailure(DatabaseException e) {
				mCacheState = STATE.FAILURE;
				switch (mRemoteState) {
				case SUCCESS:
					// should never happen
					break;
				case FAILURE:
					onFinishedReceiving();
				case RUNNING:
					// do nothing
					break;
				}
			}
		};

		// remote task
		mRemoteTask = new DatabaseConnectionTask<Result>(context) {

			@Override
			protected Result executeQuery(DatabaseConnection connection)
					throws DatabaseException {
				return CachedDatabaseConnectionTask.this
						.executeQuery(connection);
			}

			@Override
			protected void onSuccess(Result result) {
				mRemoteState = STATE.SUCCESS;
				mCacheTask.cancel(true);
				// TODO write data to cache
				onDataReceived(result, false);
				onFinishedReceiving();
			}

			@Override
			protected void onFailure(DatabaseException e) {
				mRemoteState = STATE.FAILURE;
				switch (mCacheState) {
				case SUCCESS:
					onFinishedReceiving();
					break;
				case FAILURE:
					onFinishedReceiving();
					break;
				case RUNNING:
					// do nothing
					break;
				}
			}
		};

		mRemoteConnection = new RemoteConnection(context);
		mCacheConnection = new CacheConnection();

		mCacheTask.execute(mCacheConnection);
		mRemoteTask.execute(mRemoteConnection);
	}

	// TODO better exception handling
	
	protected abstract void onDataReceived(Result result, boolean fromCache);

	protected abstract void onFinishedReceiving();

	protected abstract Result executeQuery(DatabaseConnection connection)
			throws DatabaseException;

	public void onResume() {
		mCacheTask.onResume();
		mRemoteTask.onResume();
	}

	public void onPause() {
		mCacheTask.onPause();
		mRemoteTask.onPause();
	}

}
