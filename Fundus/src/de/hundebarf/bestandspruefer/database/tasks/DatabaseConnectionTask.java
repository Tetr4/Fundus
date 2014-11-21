package de.hundebarf.bestandspruefer.database.tasks;

import android.content.Context;
import de.hundebarf.bestandspruefer.database.DatabaseConnection;
import de.hundebarf.bestandspruefer.database.DatabaseException;

public abstract class DatabaseConnectionTask<Result> extends
		ProgressDialogTask<DatabaseConnection, Void, Result> {
	private DatabaseException mException;

	public DatabaseConnectionTask(Context context) {
		super(context);
	}

	@Override
	protected Result doInBackground(DatabaseConnection... connections) {
		try {
			return executeQuery(connections[0]);
		} catch (DatabaseException e) {
			mException = e;
		}

		return null;
	}

	@Override
	protected void onPostExecute(Result result) {
		super.onPostExecute(result);

		if (result != null) {
			onSuccess(result);
		} else {
			onFailure(mException);
		}

	}

	protected abstract void onSuccess(Result result);

	protected abstract void onFailure(DatabaseException e);

	protected abstract Result executeQuery(DatabaseConnection connection)
			throws DatabaseException;

}
