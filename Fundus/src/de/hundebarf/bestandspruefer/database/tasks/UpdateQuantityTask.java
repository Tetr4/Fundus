package de.hundebarf.bestandspruefer.database.tasks;

import android.content.Context;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.DatabaseHelper;
import de.hundebarf.bestandspruefer.database.tasks.UpdateQuantityTask.OnQuantityUpdatedCallback;

public class UpdateQuantityTask extends
		ProgressDialogTask<Integer, Void, OnQuantityUpdatedCallback> {
	private int mId;
	private int mQuantity;

	public UpdateQuantityTask(Context context,
			OnQuantityUpdatedCallback callback) {
		super(context, callback);
	}

	@Override
	protected Void getResult(DatabaseHelper dbHelper, Integer... idAndQuantity)
			throws DatabaseException {
		mId = idAndQuantity[0];
		mQuantity = idAndQuantity[1];
		dbHelper.updateQuantity(mId, mQuantity);
		return null;
	}

	@Override
	protected void onSuccess(OnQuantityUpdatedCallback callback, Void v) {
		callback.onQuantityUpdated(mQuantity);
	}

	@Override
	protected void onException(OnQuantityUpdatedCallback callback,
			DatabaseException exception) {
		callback.onQuantityUpdateException(exception);
	}

	public interface OnQuantityUpdatedCallback {
		void onQuantityUpdated(int newQuantity);

		void onQuantityUpdateException(Exception exception);
	}
}