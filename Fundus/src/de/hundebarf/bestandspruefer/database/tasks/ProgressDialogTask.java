package de.hundebarf.bestandspruefer.database.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import de.hundebarf.bestandspruefer.R;

public abstract class ProgressDialogTask<Params, Progress, Result> extends
		AsyncTask<Params, Progress, Result> {
	private ProgressDialog mProgDialog;
	private boolean mDone = false;

	public ProgressDialogTask(Context context) {
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
