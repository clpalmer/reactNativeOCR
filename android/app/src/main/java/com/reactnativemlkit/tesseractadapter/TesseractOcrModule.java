package com.reactnativemlkit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.content.res.AssetManager;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.common.annotations.VisibleForTesting;

import com.googlecode.tesseract.android.TessBaseAPI;

import android.app.Application;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class TesseractOcrModule extends ReactContextBaseJavaModule {

  private final Application appContext;
  private final ReactApplicationContext reactContext;
  private TessBaseAPI tessBaseApi;

  private static final String REACT_CLASS = "TesseractOcr";

  private static String DATA_PATH = Environment.getExternalStorageDirectory().toString() + File.separator;
  private static final String TESSDATA = "tessdata";

  private static final String LANG_ENGLISH = "eng";

  public TesseractOcrModule(ReactApplicationContext reactContext, Application appContext) {
    super(reactContext);
    this.reactContext = reactContext;
    this.appContext = appContext;
    if (!this.DATA_PATH.contains(reactContext.getPackageName())) {
      this.DATA_PATH += reactContext.getPackageName() + File.separator;
    }
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("LANG_ENGLISH", LANG_ENGLISH);
    return constants;
  }

  @ReactMethod
  public void stop(Promise promise) {
    try {
      tessBaseApi.stop();
      promise.resolve("Recognition was canceled");
    } catch (Exception e) {
      Log.e(REACT_CLASS, e.toString());
      promise.reject("An error occurred", e.getMessage());
    }
  }

  @ReactMethod
  public void recognize(String path, String lang, @Nullable ReadableMap tessOptions, Promise promise) {
    prepareTesseract();

    AssetManager assetManager = this.appContext.getAssets();
    InputStream is;
    Bitmap bitmap = null;

    try {
      is = assetManager.open(path);
      bitmap = BitmapFactory.decodeStream(is);
    } catch (IOException e) {
      bitmap = BitmapFactory.decodeFile(path);

      if (bitmap == null) {
        try {
          is = appContext.getContentResolver().openInputStream(Uri.parse(path));
          bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException ex) {
          Log.d(REACT_CLASS, "Unable to open bitmap");
          promise.reject("Unable to open bitmap");
          return;
        }
      }
    }

    if (bitmap == null) {
      Log.d(REACT_CLASS, "Null bitmap");
      promise.reject("Null bitmap");
      return;
    }

    try {
      String result = extractText(bitmap, lang, tessOptions);
      promise.resolve(result);
    } catch (Exception e) {
      Log.e(REACT_CLASS, e.getMessage());
      promise.reject("An error occurred", e.getMessage());
      return;
    }
  }

  private String extractText(Bitmap bitmap, String lang, @Nullable final ReadableMap tessOptions) {
    try {
      tessBaseApi = new TessBaseAPI();
      Log.d(REACT_CLASS, "Created TessBaseAPI");
    } catch (Exception e) {
      Log.e(REACT_CLASS, e.getMessage());
      if (tessBaseApi == null) {
        Log.e(REACT_CLASS, "TessBaseAPI is null. TessFactory is not returning tess object.");
      }
    }

    Log.d(REACT_CLASS, "Initializing TessBaseAPI with data path: " + DATA_PATH);
    if (!tessBaseApi.init(DATA_PATH, lang)) {
      return "Unable to initialize TessBaseAPI";
    }
    Log.d(REACT_CLASS, "Initialized TessBaseAPI");

    if (tessOptions != null) {
      Log.d(REACT_CLASS, "tessOptions != null");

      //  //Whitelist - List of characters you want to detect
      if (tessOptions.hasKey("whitelist") && tessOptions.getString("whitelist") != null
          && !tessOptions.getString("whitelist").isEmpty()) {
        Log.d(REACT_CLASS, "Whitelist: " + tessOptions.getString("whitelist"));
        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, tessOptions.getString("whitelist"));
      }

      //  //Blacklist - List of characters you DON'T want to detect
      if (tessOptions.hasKey("blacklist") && tessOptions.getString("blacklist") != null
          && !tessOptions.getString("blacklist").isEmpty()) {
        Log.d(REACT_CLASS, "Blacklist: " + tessOptions.getString("blacklist"));
        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, tessOptions.getString("blacklist"));
      }
    }

    Log.d(REACT_CLASS, "Training file loaded");

    tessBaseApi.setImage(bitmap);

    String extractedText = "Empty result";
    try {
      extractedText = tessBaseApi.getUTF8Text();
    } catch (Exception e) {
      Log.e(REACT_CLASS, "Error in recognizing text: " + e.getMessage());
    }

    tessBaseApi.end();

    return extractedText;
  }

  private void prepareDirectory(String path) {
    File dir = new File(path);
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        Log.e(REACT_CLASS, "ERROR: Creation of directory " + path
            + " failed, check permission to write to external storage.");
      }
    } else {
      Log.i(REACT_CLASS, "Created directory " + path);
    }
  }

  private void prepareTesseract() {
    Log.d(REACT_CLASS, "Preparing tesseract enviroment");

    try {
      prepareDirectory(DATA_PATH + TESSDATA);
    } catch (Exception e) {
      e.printStackTrace();
    }

    copyTessDataFiles(TESSDATA);
  }

  private void copyTessDataFiles(String path) {
    try {
      String fileList[] = reactContext.getAssets().list(path);

      for (String fileName : fileList) {

        String pathToDataFile = DATA_PATH + path + "/" + fileName;
        if (!(new File(pathToDataFile)).exists()) {

          InputStream in = reactContext.getAssets().open(path + "/" + fileName);

          OutputStream out = new FileOutputStream(pathToDataFile);

          byte[] buf = new byte[1024];
          int len;

          while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
          }
          in.close();
          out.close();

          Log.d(REACT_CLASS, "Copied " + fileName + "to tessdata");
        }
      }
    } catch (IOException e) {
      Log.e(REACT_CLASS, "Unable to copy files to tessdata " + e.toString());
    }
  }
}