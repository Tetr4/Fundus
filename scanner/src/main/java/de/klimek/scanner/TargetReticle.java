package de.klimek.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Reticle that is used as view finder over the {@link CameraPreview}
 */
class TargetReticle extends View {
    private Paint mPaint;
    private Rect mTargetRect = new Rect();

    public TargetReticle(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(5);
    }

    void drawTargetRect(Canvas canvas) {
        double fraction = ScannerView.BOUNDS_FRACTION;
        int left = (int) (canvas.getWidth() * ((1 - fraction) / 2));
        int right = (int) (left + canvas.getWidth() * fraction);
        mTargetRect.set(left, 0, right, canvas.getHeight());
        canvas.drawRect(mTargetRect, mPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setARGB(100, 0x9F, 0xCD, 0x46); // TODO Resource
        drawTargetRect(canvas);
    }
}
