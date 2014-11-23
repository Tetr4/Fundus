package de.hundebarf.bestandspruefer.database.tasks;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import de.hundebarf.bestandspruefer.database.DatabaseConnection;
import de.hundebarf.bestandspruefer.database.DatabaseException;

public abstract class DatabaseConnectionTask<DATA> extends
		ProgressDialogTask<DatabaseConnection, Void, Void> {
	private Set<DatabaseConnection> mSuccessfulConnections = new HashSet<DatabaseConnection>();
	private DatabaseException mException;
	private DatabaseConnection mConnection;
	private DATA mResult;

	public DatabaseConnectionTask(Context context) {
		super(context);
	}

	@Override
	protected Void doInBackground(DatabaseConnection... connections) {
		for (DatabaseConnection curConnection : connections) {
			mConnection = curConnection;
			mException = null;
			mResult = null;
			try {
				mResult = executeQuery(curConnection);
				mSuccessfulConnections.add(curConnection);
			} catch (DatabaseException e) {
				mException = e;
			}
			publishProgress();
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Void... v) {
		super.onProgressUpdate(v);
		if (mResult != null) {
			onSuccess(mResult, mConnection);
		} else {
			onFailure(mException, mConnection);
		}
	}

	@Override
	protected void onPostExecute(Void v) {
		super.onPostExecute(v);
		onFinished(mSuccessfulConnections);
	}

	@Override
	protected void onCancelled() {
		onFinished(mSuccessfulConnections);
	}

	protected abstract void onSuccess(DATA data, DatabaseConnection connection);

	protected abstract void onFailure(DatabaseException e,
			DatabaseConnection connection);

	protected abstract void onFinished(
			Set<DatabaseConnection> successfulConnections);

	protected abstract DATA executeQuery(DatabaseConnection connection)
			throws DatabaseException;

}
