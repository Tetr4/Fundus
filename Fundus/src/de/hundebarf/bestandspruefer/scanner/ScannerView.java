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

	public void setCamera(Camera camera) {
		mCamera = camera;
		if (mCamera != null) {
			try {
				mCamera.setPreviewDisplay(mHolder);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (mSurfaceCreated) {
				requestLayout();
			}
		}
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
			int newHeight = getRatioAdjustedHeight(mPreviewSize, width, height);
			setMeasuredDimension(width, newHeight);
		} else {
			setMeasuredDimension(width, height);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (mCamera != null) {
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		requestLayout();
		mSurfaceCreated = true;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null) {
			try {
				mCamera.setPreviewDisplay(null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static int getRatioAdjustedHeight(Size previewSize, int width,
			int height) {
		float ratio;
		if (previewSize.height >= previewSize.width) {
			ratio = (float) previewSize.height / (float) previewSize.width;
		} else {
			ratio = (float) previewSize.width / (float) previewSize.height;
		}
		return (int) (width * ratio);
	}

}