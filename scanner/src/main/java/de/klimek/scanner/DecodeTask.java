package de.klimek.scanner;

import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

class DecodeTask extends AsyncTask<byte[], Void, Result> {
    private Decoder mDecoder;
    private Camera mCamera;
    private int mCameraDisplayOrientation;
    private MultiFormatReader mMultiFormatReader = new MultiFormatReader();

    DecodeTask(Decoder decoder, Camera camera, int cameraDisplayOrientation) {
        mDecoder = decoder;
        mCamera = camera;
        mCameraDisplayOrientation = cameraDisplayOrientation;
    }

    @Override
    protected Result doInBackground(byte[]... datas) {
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        Rect boundingRect = getBoundingRect(previewSize);
        PlanarYUVLuminanceSource source = buildLuminanceSource(datas[0], previewSize, boundingRect, mCameraDisplayOrientation);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            return mMultiFormatReader.decodeWithState(bitmap);
        } catch (NotFoundException e) {
            return null;
        } finally {
            mMultiFormatReader.reset();
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        if (result != null) {
            mDecoder.onDecodeSuccess(result.toString());
        } else {
            mDecoder.onDecodeFail();
        }
    }

    private static Rect getBoundingRect(Camera.Size previewSize) {
        double fraction = ScannerView.BOUNDS_FRACTION;
        int height = (int) (previewSize.height * fraction);
        int width = (int) (previewSize.width * fraction);
        int left = (int) (previewSize.width * ((1 - fraction) / 2));
        int top = (int) (previewSize.height * ((1 - fraction) / 2));
        int right = left + width;
        int bottom = top + height;
        return new Rect(left, top, right, bottom);
    }

    private static PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, Camera.Size previewSize, Rect boundingRect, int cameraDisplayOrientation) {
        byte[] rotatedData = rotateNV21(data, previewSize.width, previewSize.height, cameraDisplayOrientation);
        return new PlanarYUVLuminanceSource(
                rotatedData,
                previewSize.width,
                previewSize.height,
                boundingRect.left,
                boundingRect.top,
                boundingRect.width(),
                boundingRect.height(),
                false);
    }

    private static byte[] rotateNV21(byte[] yuv, int width, int height, int rotation) {
        if (rotation == 0) {
            return yuv;
        }
        boolean swap = (rotation == 90 || rotation == 270);
        boolean flipX = (rotation == 90 || rotation == 180);
        boolean flipY = (rotation == 180 || rotation == 270);

        byte[] rotated = new byte[yuv.length];
        int size = width * height;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int yIn = y * width + x;
                int uIn = size + (y >> 1) * width + (x & ~1);
                int vIn = uIn + 1;

                int wSwapped = swap ? height : width;
                int hSwapped = swap ? width : height;
                int xSwapped = swap ? y : x;
                int ySwapped = swap ? x : y;
                int xFlipped = flipX ? wSwapped - xSwapped - 1 : xSwapped;
                int yFlipped = flipY ? hSwapped - ySwapped - 1 : ySwapped;

                int yOut = yFlipped * wSwapped + xFlipped;
                int uOut = size + (yFlipped >> 1) * wSwapped + (xFlipped & ~1);
                int vOut = uOut + 1;

                rotated[yOut] = (byte) (0xff & yuv[yIn]);
                rotated[uOut] = (byte) (0xff & yuv[uIn]);
                rotated[vOut] = (byte) (0xff & yuv[vIn]);
            }
        }
        return rotated;
    }
}