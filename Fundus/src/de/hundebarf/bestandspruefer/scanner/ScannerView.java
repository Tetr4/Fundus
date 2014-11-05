package de.hundebarf.bestandspruefer.scanner;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
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
		if (mPreviewSize == null && mCamera != null) {
			mPreviewSize = mCamera.getParameters().getPreviewSize();
			// FIXME Wrong buffer size in Decoder
			// mPreviewSize = getOptimalPreviewSize(mCamera, width, height);
			Parameters params = mCamera.getParameters();
			params.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
			mCamera.setParameters(params);
		}

		/*
		 * Set height and width for the SurfaceView. Width is with of the parent
		 * (match_parent) and height depends on the aspect ratio (no
		 * stretching).
		 */
		if(mPreviewSize != null) {
			int newHeight = getPreviewHeight(mPreviewSize, width, height);
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

	private static Size getOptimalPreviewSize(Camera camera, int w, int h) {
		List<Size> sizes = camera.getParameters().getSupportedPreviewSizes();
		Size bestMatch = sizes.get(0);
		int bestWidthDiff = Integer.MAX_VALUE;
		for (Size curSize : sizes) {
			int curWidthDiff = Math.abs(curSize.width - w);
			if (curWidthDiff < bestWidthDiff) {
				bestWidthDiff = curWidthDiff;
				bestMatch = curSize;
			}
		}
		return bestMatch;
	}

	private static int getPreviewHeight(Size previewSize, int width, int height) {
		float ratio;
		if (previewSize.height >= previewSize.width)
			ratio = (float) previewSize.height / (float) previewSize.width;
		else
			ratio = (float) previewSize.width / (float) previewSize.height;
		return (int) (width * ratio);
	}

}