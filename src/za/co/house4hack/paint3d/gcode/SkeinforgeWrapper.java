package za.co.house4hack.paint3d.gcode;

import java.io.File;

import android.content.Context;
import android.os.Environment;

public class SkeinforgeWrapper {

   private static final String TAG = "Skeinforge";

  // The path to the directory containing our external storage.
   private File externalStorage;

   // The path to the directory containing the game.
   private File mPath = null;


   String mFilesDirectory = null;

  private Context mContext;

  private String mArgument;


   public SkeinforgeWrapper(Context context) {
       mContext  = context;
       mFilesDirectory = context.getFilesDir().getAbsolutePath();

       externalStorage = new File(Environment.getExternalStorageDirectory(), context.getPackageName());

       mPath = context.getFilesDir();
       mArgument = mPath.getAbsolutePath();

   }
//
//   /**
//    * This determines if unpacking one the zip files included in
//    * the .apk is necessary. If it is, the zip file is unpacked.
//    */
//   public void unpackData(final String resource, File target) {
//
//       // If the disk data is out of date, extract it and write the
//       // version file.
//       Log.v("python", "Extracting " + resource + " assets.");
//
//       recursiveDelete(target);
//       target.mkdirs();
//
//       AssetExtract ae = new AssetExtract(mContext);
//       ae.extractTar(resource + ".mp3", target.getAbsolutePath());
//   }

   public void generateGcode(String file) {
      //TODO: parameter file is ignored
//
//       unpackData("private", mContext.getFilesDir());
//       Log.d(TAG,mContext.getFilesDir().getAbsolutePath());
//       unpackData("public", externalStorage);
//       Log.d(TAG,externalStorage.getAbsolutePath());

      System.loadLibrary("python2.7");
      System.loadLibrary("application");

      //System.load(mContext.getFilesDir() + "/lib/python2.7/lib-dynload/_io.so");
      //System.load(mContext.getFilesDir() + "/lib/python2.7/lib-dynload/unicodedata.so");

      nativeSetEnv("ANDROID_PRIVATE", mFilesDirectory);
      nativeSetEnv("ANDROID_ARGUMENT", mArgument);
      nativeSetEnv("PYTHONOPTIMIZE", "2");
      nativeSetEnv("PYTHONHOME", mFilesDirectory);
      nativeSetEnv("PYTHONPATH", mArgument + ":" + mFilesDirectory + "/lib");
      nativeInit();

   }
  // Native part

   public static native void nativeSetEnv(String name, String value);
   public static native void nativeInit();

}
