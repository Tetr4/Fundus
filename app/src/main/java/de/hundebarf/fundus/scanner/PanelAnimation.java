package de.hundebarf.fundus.scanner;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class PanelAnimation extends Animation {
    private int mStartHeight;
    private int mDeltaHeight;
	private View mView;

    public PanelAnimation(View view, int startHeight, int endHeight) {
    	mView = view;
        mStartHeight = startHeight;
        mDeltaHeight = endHeight - startHeight;
    }

    @Override
    protected void applyTransformation(float interpolatedTime,
                                             Transformation t) {
        LayoutParams lp = mView.getLayoutParams();
        lp.height = (int) (mStartHeight + mDeltaHeight * interpolatedTime);
        mView.setLayoutParams(lp);
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}
