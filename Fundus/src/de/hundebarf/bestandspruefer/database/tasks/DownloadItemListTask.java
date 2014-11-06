package de.hundebarf.bestandspruefer.database.tasks;

import java.util.List;

import android.content.Context;
import de.hundebarf.bestandspruefer.collection.Item;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.DatabaseHelper;
import de.hundebarf.bestandspruefer.database.tasks.DownloadItemListTask.OnItemListDownloadedCallback;

public class DownloadItemListTask extends
		ProgressDialogTask<Void, List<Item>, OnItemListDownloadedCallback> {

	public DownloadItemListTask(Context context,
			OnItemListDownloadedCallback callback) {
		super(context, callback);
	}

	@Override
	protected List<Item> getResult(DatabaseHelper dbHelper, Void... v)
			throws DatabaseException {
		return dbHelper.queryItemList();
	}

	@Override
	protected void onSuccess(OnItemListDownloadedCallback callback,
			List<Item> result) {
		callback.onItemListDownloaded(result);
	}

	@Override
	protected void onException(OnItemListDownloadedCallback callback,
			DatabaseException exception) {
		callback.onItemListDownloadException(exception);
	}

	public interface OnItemListDownloadedCallback {
		void onItemListDownloaded(List<Item> items);

		void onItemListDownloadException(Exception exception);
	}

}
