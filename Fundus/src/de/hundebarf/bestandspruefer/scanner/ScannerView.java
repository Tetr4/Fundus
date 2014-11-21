package de.hundebarf.bestandspruefer.scanner;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ScannerView extends SurfaceView implements SurfaceHolder.Callback {
	public static final String TAG = ScannerView.class.getSimpleName();

	private SurfaceHolder mHolder;
	boolean mSurfaceCreated = false;

	private Camera mCamera;
	private Size mPreviewSize;

	public ScannerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mHolder = getHolder();
		mHolder.addCallback(this);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int width = resolveSize(getSuggestedMinimumWidth(),
				widthMeasureSpec);
		final int height = resolveSize(getSuggestedMinimumHeight(),
				heightMeasureSpec);

		// get camera previewsize
		if (mPreviewSize == null && mCamera != null) {
			mPreviewSize = mCamera.getParameters().getPreviewSize();
		}

		/*
		 * Set height and width for the SurfaceView. Width is given and height
		 * depends on the aspect ratio (no stretching).
		 */
		if (mPreviewSize != null) {
			float ratio = (float) mPreviewSize.width / (float) mPreviewSize.height;
			int newHeight = (int) (width * ratio);
			setMeasuredDimension(width, newHeight);
		} else {
			setMeasuredDimension(width, height);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mSurfaceCreated = true;
		startPreview(mCamera);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		requestLayout();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	public void startPreview(Camera camera) {
		mCamera = camera;
		if (mCamera != null && mSurfaceCreated) {
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void stopPreview() {
		if (mCamera != null) {
			mCamera.stopPreview();
		}
		mCamera = null;
		mPreviewSize = null;
		mSurfaceCreated = false;
	}
}