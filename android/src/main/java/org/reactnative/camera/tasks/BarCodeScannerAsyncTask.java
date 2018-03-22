package org.reactnative.camera.tasks;

import android.support.v4.view.ViewCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import org.reactnative.camera.RNCameraView;

public class BarCodeScannerAsyncTask extends android.os.AsyncTask<Void, Void, Result> {
  private byte[] mImageData;
  private int mWidth;
  private int mHeight;
  private BarCodeScannerAsyncTaskDelegate mDelegate;
  private final MultiFormatReader mMultiFormatReader;
  private int screenWith = -1;
  private int screenHight = -1;
  RNCameraView rnCameraView;
  //  note(sjchmiela): From my short research it's ok to ignore rotation of the image.
  public BarCodeScannerAsyncTask(
      BarCodeScannerAsyncTaskDelegate delegate,
      MultiFormatReader multiFormatReader,
      byte[] imageData,
      int width,
      int height
  ) {
    mImageData = imageData;
    mWidth = width;
    mHeight = height;
    mDelegate = delegate;
    mMultiFormatReader = multiFormatReader;

    if (delegate instanceof RNCameraView) {
      rnCameraView = (RNCameraView) delegate;
      DisplayMetrics dm = rnCameraView.getResources().getDisplayMetrics();
      screenWith = (int)(dm.widthPixels/dm.density);
      screenHight = (int)(dm.heightPixels/dm.density);
    }
  }

  @Override
  protected Result doInBackground(Void... ignored) {
    if (isCancelled() || mDelegate == null) {
      return null;
    }

    Result result = null;

    try {
      BinaryBitmap bitmap = generateBitmapFromImageData(mImageData, mWidth, mHeight);
      result = mMultiFormatReader.decodeWithState(bitmap);
    } catch (NotFoundException e) {
      // No barcode found, result is already null.
    } catch (Throwable t) {
      t.printStackTrace();
    }

    return result;
  }

  @Override
  protected void onPostExecute(Result result) {
    super.onPostExecute(result);
    if (result != null) {
      mDelegate.onBarCodeRead(result);
    }
    mDelegate.onBarCodeScanningTaskCompleted();
  }

  private BinaryBitmap generateBitmapFromImageData(byte[] imageData, int width, int height) {
//    PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
//        imageData, // byte[] yuvData
//        width, // int dataWidth
//        height, // int dataHeight
//        0, // int left
//        0, // int top
//        width, // int width
//        height, // int height
//        false // boolean reverseHorizontal
//    );
//    return new BinaryBitmap(new HybridBinarizer(source));
    int scannerLeft = 0;
    int scannerTop = 0;
    int scannerWidth = width;
    int scannerHeight = height;
    if (rnCameraView != null){
      Display display = ViewCompat.getDisplay(rnCameraView);
      int wRatio = width/screenHight;
      int hRatio = height/screenWith;
      if (display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180 ){
        scannerHeight = rnCameraView.getBarCodeScannerWidth() >0 ? rnCameraView.getBarCodeScannerWidth() * hRatio : height;
        scannerWidth = rnCameraView.getBarCodeScannerHeight() >0 ? rnCameraView.getBarCodeScannerHeight() * wRatio : width;
      } else {
        scannerWidth = rnCameraView.getBarCodeScannerWidth() >0 ? rnCameraView.getBarCodeScannerWidth() * wRatio : width;
        scannerHeight = rnCameraView.getBarCodeScannerHeight() >0 ? rnCameraView.getBarCodeScannerHeight() *  hRatio : height;
      }
      // 暂时只实现扫描框剧中的
      scannerLeft = (width - scannerWidth)/2 - 1;
      scannerTop = (height - scannerHeight)/2 - 1;
    }
    PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
            imageData, // byte[] yuvData
            width, // int dataWidth
            height, // int dataHeight
            scannerLeft, // int left
            scannerTop, // int top
            scannerWidth, // int width
            scannerHeight, // int height
            false // boolean reverseHorizontal
    );
    return new BinaryBitmap(new HybridBinarizer(source));
  }

}
