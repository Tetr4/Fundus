package de.hundebarf.bestandspruefer.database.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import de.hundebarf.bestandspruefer.R;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.DatabaseHelper;

public abstract class ProgressDialogTask<PARAM, RESULT, CALLBACK> extends
		AsyncTask<PARAM, Void, RESULT> {
	private ProgressDialog mProgDialog;
	private CALLBACK mCallback;
	private DatabaseException mException;
	private Context mContext;
	private boolean mDone = false;

	public ProgressDialogTask(Context context, CALLBACK callback) {
		mContext = context;
		mCallback = callback;

		mProgDialog = new ProgressDialog(context);
		mProgDialog.setMessage(context.getResources().getString(
				R.string.query_info));
		mProgDialog.setIndeterminate(false);
		mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgDialog.setCancelable(false);
	}

	protected void onPreExecute() {
		mProgDialog.show();
	}

	@Override
	protected RESULT doInBackground(PARAM... params) {
		try {
			DatabaseHelper dbHelper = new DatabaseHelper(mContext);
			return getResult(dbHelper, params);
		} catch (DatabaseException e) {
			mException = e;
			return null;
		}
	}

	protected void onPostExecute(RESULT result) {
		if (mProgDialog.isShowing()) {
			mProgDialog.dismiss();
		}
		mDone = true;

		if (mException != null) {
			onException(mCallback, mException);
		} else {
			onSuccess(mCallback, result);
		}
	}

	public void onResume() {
		if (!mProgDialog.isShowing() && !mDone) {
			mProgDialog.show();
		}
	}

	public void onPause() {
		if (mProgDialog.isShowing()) {
			mProgDialog.dismiss();
		}
	}

	protected abstract void onSuccess(CALLBACK callback, RESULT result);

	protected abstract void onException(CALLBACK callback,
			DatabaseException exception);

	protected abstract RESULT getResult(DatabaseHelper dbHelper,
			PARAM... params) throws DatabaseException;

}
