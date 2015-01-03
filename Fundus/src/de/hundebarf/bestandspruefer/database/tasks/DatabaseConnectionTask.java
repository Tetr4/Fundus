package de.hundebarf.bestandspruefer.database.tasks;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.DialogInterface;
import de.hundebarf.bestandspruefer.collection.QueryResult;
import de.hundebarf.bestandspruefer.database.DatabaseConnection;
import de.hundebarf.bestandspruefer.database.DatabaseException;

public abstract class DatabaseConnectionTask<DATA> extends
		ProgressDialogTask<DatabaseConnection, QueryResult<DATA>, Void> {
	private Set<DatabaseConnection> mSuccessfulConnections = new HashSet<DatabaseConnection>();

	public DatabaseConnectionTask(Context context) {
		super(context);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Void doInBackground(DatabaseConnection... connections) {
		for (DatabaseConnection curConnection : connections) {
			QueryResult<DATA> result = new QueryResult<DATA>();
			result.connection = curConnection;
			try {
				result.data = executeQuery(curConnection);
			} catch (DatabaseException e) {
				result.exception = e;
			}
			publishProgress(result);
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(QueryResult<DATA>... results) {
		QueryResult<DATA> result = results[0];
		if (result.data != null) {
			mSuccessfulConnections.add(result.connection);
			onSuccess(result.data, result.connection);
		} else {
			onFailure(result.exception, result.connection);
		}
	}

	@Override
	protected void onPostExecute(Void v) {
		super.onPostExecute(v);
		onFinished(mSuccessfulConnections);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
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
