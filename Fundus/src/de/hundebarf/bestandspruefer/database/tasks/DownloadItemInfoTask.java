package de.hundebarf.bestandspruefer.database.tasks;

import android.content.Context;
import de.hundebarf.bestandspruefer.collection.Item;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.DatabaseHelper;
import de.hundebarf.bestandspruefer.database.tasks.DownloadItemInfoTask.OnItemInfoDownloadedCallback;

public class DownloadItemInfoTask extends
		ProgressDialogTask<Integer, Item, OnItemInfoDownloadedCallback> {

	public DownloadItemInfoTask(Context context,
			OnItemInfoDownloadedCallback callback) {
		super(context, callback);
	}

	@Override
	protected Item getResult(DatabaseHelper dbHelper, Integer... ids)
			throws DatabaseException {
		return dbHelper.queryItem(ids[0]);
	}

	@Override
	protected void onSuccess(OnItemInfoDownloadedCallback callback, Item item) {
		callback.onItemInfoDownloaded(item);
	}

	@Override
	protected void onException(OnItemInfoDownloadedCallback callback,
			DatabaseException exception) {
		callback.onItemInfoDownloadException(exception);
	}

	public static interface OnItemInfoDownloadedCallback {
		void onItemInfoDownloaded(Item item);

		void onItemInfoDownloadException(Exception exception);
	}
}