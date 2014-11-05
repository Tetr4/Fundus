package de.hundebarf.bestandspruefer.database.tasks;

import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import de.hundebarf.bestandspruefer.R;
import de.hundebarf.bestandspruefer.collection.Item;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.DatabaseHelper;

public class DownloadItemListTask extends AsyncTask<Void, Void, List<Item>> {
	private ProgressDialog mProgDialog;
	private OnItemListDownloadedCallback mCallback;
	private DatabaseException mException;

	public DownloadItemListTask(Context context,
			OnItemListDownloadedCallback callback) {
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
	protected List<Item> doInBackground(Void... v) {
		try {
			DatabaseHelper dbHelper = new DatabaseHelper();
			return dbHelper.queryItemList();
		} catch (DatabaseException e) {
			mException = e;
			return null;
		}
	}

	protected void onPostExecute(List<Item> items) {
		dismissProgressDialog();

		if (mException != null) {
			mCallback.onItemListDownloadException(mException);
		} else {
			mCallback.onItemListDownloaded(items);
		}
	}

	public void dismissProgressDialog() {
		if (mProgDialog.isShowing()) {
			mProgDialog.dismiss();
		}
	}

	public interface OnItemListDownloadedCallback {
		void onItemListDownloaded(List<Item> items);

		void onItemListDownloadException(Exception exception);
	}

}