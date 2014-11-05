package de.hundebarf.bestandspruefer.database.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import de.hundebarf.bestandspruefer.R;
import de.hundebarf.bestandspruefer.collection.Item;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.DatabaseHelper;

public class DownloadItemInfoTask extends AsyncTask<Integer, Void, Item> {
	private ProgressDialog mProgDialog;
	private OnItemInfoDownloadedCallback mCallback;
	private DatabaseException mException;
	private Context mContext;
	
	public DownloadItemInfoTask(Context context,
			OnItemInfoDownloadedCallback callback) {
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
	protected Item doInBackground(Integer... ids) {
		try {
			DatabaseHelper dbHelper = new DatabaseHelper(mContext);
			return dbHelper.queryItem(ids[0]);
		} catch (DatabaseException e) {
			mException = e;
			return null;
		}
	}

	protected void onPostExecute(Item item) {
		dismissProgressDialog();

		if (mException != null) {
			mCallback.onItemInfoDownloadException(mException);
		} else {
			mCallback.onItemInfoDownloaded(item);
		}
	}

	public void dismissProgressDialog() {
		if (mProgDialog.isShowing()) {
			mProgDialog.dismiss();
		}
	}

	public static interface OnItemInfoDownloadedCallback {
		void onItemInfoDownloaded(Item item);

		void onItemInfoDownloadException(Exception exception);
	}

}