package com.reactnativemlkit;

import android.app.Application;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;
import android.net.Uri;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Point;
import android.util.Log;

public class MLKitModule extends ReactContextBaseJavaModule {
  private final Application appContext;
  private final ReactApplicationContext reactContext;

  private static final String REACT_CLASS = "MLKit";
  private static final String SOME_CONSTANT_KEY = "SOME_KEY";
  private static final String LOG_TAG = "[***** MLKIT *****] ";

  public MLKitModule(ReactApplicationContext reactContext, Application appContext) {
    super(reactContext);
    this.reactContext = reactContext;
    this.appContext = appContext;
  }

  @ReactMethod
  public void detectInImage(String filePath, final Promise promise) {
    FirebaseVisionTextDetector detector = FirebaseVision.getInstance().getVisionTextDetector();
    
    AssetManager assetManager = this.appContext.getAssets();

    InputStream is;
    Bitmap bitmap = null;
    try {
      is = assetManager.open(filePath);
      bitmap = BitmapFactory.decodeStream(is);
    } catch (IOException e) {
      e.printStackTrace();
      Log.d(LOG_TAG, "IOException reading file");
      promise.reject("IOException: " + e.toString() + " - " + Arrays.toString(this.appContext.fileList()));
      return;
    }

    if (bitmap == null) {
      Log.d(LOG_TAG, "Null bitmap");
      promise.reject("Null bitmap");
      return;
    }

    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
    Task<FirebaseVisionText> result = detector.detectInImage(image)
      .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
        @Override
        public void onSuccess(FirebaseVisionText firebaseVisionText) {
          Log.d(LOG_TAG, "Success");
          sendResult(firebaseVisionText, promise);
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(Exception e) {
          Log.d(LOG_TAG, "Failure");
          promise.reject(e.toString());
        }
      });

    if (result == null) {
      Log.d(LOG_TAG, "Null task");
      promise.reject("detectInImage returned null");
    }
  }

  private Bitmap getBitmapFromAsset(String filePath) {
      AssetManager assetManager = this.appContext.getAssets();

      InputStream is;
      Bitmap bitmap = null;
      try {
          is = assetManager.open(filePath);
          bitmap = BitmapFactory.decodeStream(is);
      } catch (IOException e) {
          e.printStackTrace();
      }

      return bitmap;
  }

  private void sendResult(FirebaseVisionText firebaseVisionText, Promise promise) {
    WritableArray resp = new WritableNativeArray();
    for (FirebaseVisionText.Block block: firebaseVisionText.getBlocks()) {
      WritableMap blockMap = new WritableNativeMap();

      Rect boundingBox = block.getBoundingBox();
      WritableMap rectMap = new WritableNativeMap();
      rectMap.putInt("left", boundingBox.left);
      rectMap.putInt("right", boundingBox.right);
      rectMap.putInt("top", boundingBox.top);
      rectMap.putInt("bottom", boundingBox.bottom);
      blockMap.putMap("boundingBox", rectMap);

      Point[] cornerPoints = block.getCornerPoints();
      WritableMap pointsMap = new WritableNativeMap();
      pointsMap.putInt("topLeftX", cornerPoints[0].x);
      pointsMap.putInt("topLeftY", cornerPoints[0].y);
      pointsMap.putInt("topRightX", cornerPoints[1].x);
      pointsMap.putInt("topRightY", cornerPoints[1].y);
      pointsMap.putInt("bottomRightX", cornerPoints[2].x);
      pointsMap.putInt("bottomRightY", cornerPoints[2].y);
      pointsMap.putInt("bottomLeftX", cornerPoints[3].x);
      pointsMap.putInt("bottomLeftY", cornerPoints[3].y);
      blockMap.putMap("cornerPoints", pointsMap);

      String blockText = block.getText();
      Log.d(LOG_TAG, blockText);

      WritableArray linesArray = new WritableNativeArray();
      for (FirebaseVisionText.Line line: block.getLines()) {
        linesArray.pushString(line.getText());
          // for (FirebaseVisionText.Element element: line.getElements()) {
          //     // ...
          // }
      }
      blockMap.putArray("lines", linesArray);

      resp.pushMap(blockMap);
    }
  
    promise.resolve(resp);
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put(SOME_CONSTANT_KEY, "23");
    return constants;
  }
}