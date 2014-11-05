package de.hundebarf.bestandspruefer.database.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import de.hundebarf.bestandspruefer.R;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.DatabaseHelper;

public class UpdateQuantityTask extends AsyncTask<Integer, Void, Void> {
	private ProgressDialog mProgDialog;
	private OnQuantityUpdatedCallback mCallback;
	private DatabaseException mException;
	private int mQuantity;
	private int mId;
	private Context mContext;

	public UpdateQuantityTask(Context context,
			OnQuantityUpdatedCallback callback) {
		mContext = context;
		mCallback = callback;

		mProgDialog = new ProgressDialog(context);
		mProgDialog.setMessage(context.getResources().getString(
				R.string.query_info));
		mProgDialog.setIndeterminate(false);
		mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgDialog.setCancelable(false);
		mProgDialog.show();
	}

	protected void onPreExecute() {
		mProgDialog.show();
	}

	@Override
	protected Void doInBackground(Integer... idAndQuantity) {
		mId = idAndQuantity[0];
		mQuantity = idAndQuantity[1];
		try {
			DatabaseHelper dbHelper = new DatabaseHelper(mContext);
			dbHelper.updateQuantity(mId, mQuantity);
		} catch (DatabaseException e) {
			mException = e;
		}
		return null;
	}

	protected void onPostExecute(Void v) {
		dismissProgressDialog();

		if (mException != null) {
			mCallback.onQuantityUpdateException(mException);
		} else {
			mCallback.onQuantityUpdated(mQuantity);
		}
	}

	public void dismissProgressDialog() {
		if (mProgDialog.isShowing()) {
			mProgDialog.dismiss();
		}
	}

	public interface OnQuantityUpdatedCallback {
		void onQuantityUpdated(int newQuantity);

		void onQuantityUpdateException(Exception exception);
	}

}