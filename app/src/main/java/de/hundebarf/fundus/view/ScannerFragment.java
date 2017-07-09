package de.hundebarf.fundus.view;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import de.hundebarf.fundus.R;
import de.klimek.scanner.OnDecodedCallback;
import de.klimek.scanner.ScannerView;

/**
 * Collapsible fragment, which starts/stops the scanner after expanding/collapsing.
 */
public class ScannerFragment extends Fragment {
    private FrameLayout mPanel;
    private ScannerView mScannerView;

    private PanelAnimation mCollapseAnimation;
    private PanelAnimation mExpandAnimation;
    private int mScannerHeightExpanded;
    private int mScannerHeightCollapsed;
    private boolean mExpanded = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scanner, container, false);
        mPanel = (FrameLayout) rootView.findViewById(R.id.panel);
        mScannerView = (ScannerView) rootView.findViewById(R.id.scanner);
        initPanelAnimations();
        return rootView;
    }

    private void initPanelAnimations() {
        mScannerHeightExpanded = (int) getResources().getDimension(R.dimen.scanner_height_expanded);
        mScannerHeightCollapsed = (int) getResources().getDimension(R.dimen.scanner_height_collapsed);

        // expand animation
        mExpandAnimation = new PanelAnimation(mPanel, mScannerHeightCollapsed, mScannerHeightExpanded);
        mExpandAnimation.setDuration(200);
        mExpandAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mScannerView.startScanning();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mExpanded = true;
            }
        });

        // collapse animation
        mCollapseAnimation = new PanelAnimation(mPanel, mScannerHeightExpanded, mScannerHeightCollapsed);
        mCollapseAnimation.setDuration(200);
        mCollapseAnimation.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mScannerView.stopScanning();
                mExpanded = false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mExpanded) {
            mScannerView.startScanning();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopScanning();
    }

    public void expand() {
        if (!isExpanded()) {
            // calls startCamera() when starting
            mPanel.startAnimation(mExpandAnimation);
        }
    }

    public void collapse() {
        if (isExpanded()) {
            // calls stopCamera() when finished
            mPanel.startAnimation(mCollapseAnimation);
        }
    }

    public void collapseNoAnim() {
        // collapse panel instantly
        ViewGroup.LayoutParams lp = mPanel.getLayoutParams();
        lp.height = mScannerHeightCollapsed;
        mExpanded = false;
        mScannerView.stopScanning();
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    public void setOnDecodedCallback(OnDecodedCallback callback) {
        mScannerView.setOnDecodedCallback(callback);
    }
}
