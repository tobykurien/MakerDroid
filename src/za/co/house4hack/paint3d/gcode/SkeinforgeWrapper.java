package za.co.house4hack.paint3d.gcode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import za.co.house4hack.paint3d.Main;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

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

       externalStorage = new File(Environment.getExternalStorageDirectory(), Main.SKEINFORGE_DIR);

       mPath = context.getFilesDir();
       mArgument = mPath.getAbsolutePath();

   }
//
//   /**
//    * This determines if unpacking one the zip files included in
//    * the .apk is necessary. If it is, the zip file is unpacked.
//    */
   public void unpackData(final String resource, File target) {

       // If the disk data is out of date, extract it and write the
       // version file.
       Log.v("python", "Extracting " + resource + " assets.");

       recursiveDelete(target);
       target.mkdirs();

       AssetExtract ae = new AssetExtract(mContext);
       ae.extractTar(resource + ".mp3", target.getAbsolutePath());
   }

   public void recursiveDelete(File f) {
       if (f.isDirectory()) {
           for (File r : f.listFiles()) {
               recursiveDelete(r);
           }
       }
       f.delete();
   }
   
   public void generateGcode(String file) {
      //TODO: parameter file is ignored
//
       unpackData("private", mContext.getFilesDir());
       unpackData("public", externalStorage);
       /* try {
    	  // copySkeinforge(mContext, externalStorage.getAbsolutePath());
       } catch (IOException e) {
    	   Log.e(Main.LOG_TAG, "Error copying skeinforge", e);
       }*/

      System.loadLibrary("python2.7");
      System.loadLibrary("application");

      //System.load(mContext.getFilesDir() + "/lib/python2.7/lib-dynload/_io.so");
      //System.load(mContext.getFilesDir() + "/lib/python2.7/lib-dynload/unicodedata.so");

      nativeSetEnv("ANDROID_PRIVATE", mFilesDirectory);
      nativeSetEnv("ANDROID_ARGUMENT", mArgument);
      nativeSetEnv("PYTHONOPTIMIZE", "2");
      nativeSetEnv("PYTHONHOME", mFilesDirectory);
      nativeSetEnv("PYTHONPATH", mArgument + ":" + mFilesDirectory + "/lib");
      nativeSetEnv("SETTINGS_DIRECTORY", externalStorage.getAbsolutePath()+"/settings");
      //nativeInit();
      runPython("import sys, posix, os \n" +
    	        "private = posix.environ['ANDROID_PRIVATE']\n" +
    	        "argument = posix.environ['ANDROID_ARGUMENT']\n" +
    	        "sys.path[:] = [ \n" +
    			"    private + '/lib/python27.zip', \n" +
    			"    private + '/lib/python2.7/', \n" +
    			"    private + '/lib/python2.7/lib-dynload/', \n" +
    			"    private + '/lib/python2.7/site-packages/', \n" +
    			"    argument ]\n" +
    	        "import androidembed\n" +
    	        "class LogFile(object):\n" +
    	        "    def __init__(self):\n" +
    	        "        self.buffer = ''\n" +
    	        "    def write(self, s):\n" +
    	        "        s = self.buffer + s\n" +
    	        "        lines = s.split(\"\\n\")\n" +
    	        "        for l in lines[:-1]:\n" +
    	        "            androidembed.log(l)\n" +
    	        "        self.buffer = lines[-1]\n" +
    	        "sys.stdout = sys.stderr = LogFile()\n" +
    			"import site; print site.getsitepackages()\n"+
    			"print 'Android path', sys.path\n" +
    	        "print 'Android kivy bootstrap done. __name__ is', __name__\n" +
    			"os.chdir('"+externalStorage.getParent()+"') \n" +
    	        "sys.path.append('"+externalStorage.getAbsolutePath()+"')\n" + 
    	        "sys.path.append('"+externalStorage.getAbsolutePath()+"/skeinforge/skeinforge_tools')\n" +
    	        "import craft\n" + 
    	        "craft.writeOutput('"+externalStorage.getParent() + "/paint3d.stl')");
      
   }
  
   // copy skienforge from assets to sd card (can't execute from the assets folder)
  private void copySkeinforge(Context context, String absolutePath) throws IOException {
	  copyDirFile(context, absolutePath, "Skeinforge");
  }
  
  // recursively copy dir/files
  private void copyDirFile(Context context, String targetDir, String path) throws IOException {
	  Log.d(Main.LOG_TAG, "copying " + path);
	  try {
		  String[] files = context.getAssets().list(path);
		  if (files.length > 0) {
			  new File(targetDir + "/" + path).mkdirs();
		  }
		  for (String file : files) {
			  copyDirFile(context, targetDir, path + "/" + file);
		  }		  
	  } catch (IOException e) {
		  Log.d(Main.LOG_TAG, "excption " + e.getMessage());
	  }
	  
	  try {
		  File t = new File(targetDir +'/'+ path);
		  FileOutputStream fos = new FileOutputStream(t);
		  InputStream is = context.getAssets().open(path);
		  int len = 0;
		  byte buf[] = new byte[8120];
		  while ((len = is.read(buf)) > 0) {
			  fos.write(buf, 0, len);
		  }
		  is.close();
		  fos.close();	  		  
	  } catch (FileNotFoundException e) {
		  Log.e(Main.LOG_TAG, "error copying " + path, e);
	  }
  }
  
// Native part

   public static native void nativeSetEnv(String name, String value);
   public static native void nativeInit();
   public static native void runPython(String script);

}
