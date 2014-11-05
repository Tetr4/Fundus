package de.hundebarf.bestandspruefer.scanner;

import android.app.Fragment;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;
import de.hundebarf.bestandspruefer.R;
import de.hundebarf.bestandspruefer.scanner.Decoder.OnDecodedCallback;

public class ScannerFragment extends Fragment implements AnimationListener {
	static final double BOUNDS_FRACTION = 0.6; //
	static final double VERTICAL_HEIGHT_FRACTION = 0.3;
	private static final String TAG = ScannerFragment.class.getSimpleName();

	private FrameLayout mScannerPanel;
	private ScannerView mScannerView;
	private TargetReticle mTargetReticle;
	private Camera mCamera;
	private int mCameraId;
	private CameraInfo mCameraInfo;

	private AsyncTask<Void, Void, Exception> mStartCameraTask;

	private boolean mExpanded = false;
	private PanelAnimation mCollapseAnimation;
	private PanelAnimation mExpandAnimation;

	private Decoder mDecoder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCameraId = findBestCamera();
		mCameraInfo = new CameraInfo();
		Camera.getCameraInfo(mCameraId, mCameraInfo);

		mDecoder = new Decoder(getActivity());
	}

	private static int findBestCamera() {
		// TODO find cameraID for best camera with focus
		int cameraId = 0;
		// CameraInfo cameraInfo = new CameraInfo();
		// getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
		// CameraInfo cameraInfo = new CameraInfo();
		// for (int curCameraId = 0; curCameraId < Camera.getNumberOfCameras();
		// curCameraId++) {
		// Camera.getCameraInfo(curCameraId, cameraInfo);
		// if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
		// }
		// }
		return cameraId;
	}

	private void initPanelAnimations() {
		int scannerHeight = (int) getResources().getDimension(
				R.dimen.scanner_height);
		mExpandAnimation = new PanelAnimation(mScannerPanel, 0, scannerHeight);
		mExpandAnimation.setDuration(200);
		mExpandAnimation.setAnimationListener(this);
		mCollapseAnimation = new PanelAnimation(mScannerPanel, scannerHeight, 0);
		mCollapseAnimation.setDuration(200);
		mCollapseAnimation.setAnimationListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_scanner, container,
				false);
		mScannerPanel = (FrameLayout) rootView.findViewById(R.id.scanner_panel);
		mScannerView = (ScannerView) rootView.findViewById(R.id.scanner_view);
		mTargetReticle = (TargetReticle) rootView
				.findViewById(R.id.target_reticle);

		initPanelAnimations();

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopCamera();

		// collapse panel without animation
		LayoutParams lp = mScannerPanel.getLayoutParams();
		lp.height = 0;
		mExpanded = false;
	}

	private static int getCameraDisplayOrientation(Display display,
			CameraInfo cameraInfo) {
		int rotation = display.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		int result;
		if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
			result = (cameraInfo.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (cameraInfo.orientation - degrees + 360) % 360;
		}
		return result;
	}

	private void startCamera() {
		// Task for smooth UI while camera loads
		mStartCameraTask = new AsyncTask<Void, Void, Exception>() {

			@Override
			protected Exception doInBackground(Void... v) {
				try {
					mCamera = Camera.open(mCameraId);
				} catch (RuntimeException e) {
					return e;
				}
				// set params
				Parameters params = mCamera.getParameters();
				params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				params.setFlashMode(Parameters.FLASH_MODE_OFF);
				params.setRecordingHint(true);
				mCamera.setParameters(params);
				return null;
			}

			@Override
			protected void onPostExecute(Exception e) {
				if (e != null) {
					Log.w(TAG,
							"Exception while opening camera: " + e.getMessage());
					mCamera = null;
					// collapse panel without animation
					LayoutParams lp = mScannerPanel.getLayoutParams();
					lp.height = 0;
					mExpanded = false;
					return;
				}
				Display display = getActivity().getWindowManager()
						.getDefaultDisplay();
				int displayOrientation = getCameraDisplayOrientation(display,
						mCameraInfo);
				mCamera.setDisplayOrientation(displayOrientation);
				mScannerView.setCamera(mCamera);
				mCamera.startPreview();
				mDecoder.startDecoding(mCamera, displayOrientation);
			}
		}.execute();
	}

	private void stopCamera() {
		if (mStartCameraTask != null) {
			mStartCameraTask.cancel(true);
		}
		if (mCamera != null) {
			mDecoder.stopDecoding(mCamera);
			mCamera.stopPreview();
			mScannerView.setCamera(null);
			mCamera.release();
			mCamera = null;
		}
	}

	public void expand() {
		if (!isExpanded()) {
			// calls startCamera() in onAnimationStart();
			mScannerPanel.startAnimation(mExpandAnimation);
		}
	}

	public void collapse() {
		if (isExpanded()) {
			// calls stopCamera() in onAnimationStop();
			mScannerPanel.startAnimation(mCollapseAnimation);
		}
	}

	public boolean isExpanded() {
		return mExpanded;
	}

	public void setOnDecodedCallback(OnDecodedCallback callback) {
		mDecoder.setOnDecodedCallback(callback);
	}

	@Override
	public void onAnimationStart(Animation animation) {
		if (animation == mExpandAnimation) {
			startCamera();
		}
	}

	@Override
	public void onAnimationRepeat(Animation animation) {

	}

	@Override
	public void onAnimationEnd(Animation animation) {
		if (animation == mCollapseAnimation) {
			stopCamera();
			mExpanded = false;
		} else if (animation == mExpandAnimation) {
			mExpanded = true;
		}
	}

}
