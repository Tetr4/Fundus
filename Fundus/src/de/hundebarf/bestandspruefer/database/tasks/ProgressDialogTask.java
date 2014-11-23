package de.hundebarf.bestandspruefer.database.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import de.hundebarf.bestandspruefer.R;

public abstract class ProgressDialogTask<Params, Progress, Result> extends
		AsyncTask<Params, Progress, Result> implements OnCancelListener {
	private ProgressDialog mProgDialog;
	private boolean mDone = false;

	public ProgressDialogTask(Context context) {
		mProgDialog = new ProgressDialog(context);
		mProgDialog.setMessage(context.getResources().getString(
				R.string.query_info));
		mProgDialog.setIndeterminate(false);
		mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgDialog.setOnCancelListener(this);
		mProgDialog.setCancelable(true);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		// dialog cancelled -> cancel task
		cancel(true);
	}

	@Override
	protected void onPreExecute() {
		mProgDialog.show();
	}

	@Override
	protected void onPostExecute(Result result) {
		if (mProgDialog.isShowing()) {
			mProgDialog.dismiss();
		}
		mDone = true;
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
}
