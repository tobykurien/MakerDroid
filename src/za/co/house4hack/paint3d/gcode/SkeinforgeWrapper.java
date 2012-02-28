package za.co.house4hack.paint3d.gcode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import za.co.house4hack.paint3d.Main;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class SkeinforgeWrapper {

	private static final String TAG = "Skeinforge";

	private static final String SKEINFORGE_START = "Starting Skeinforge";

	private static final int GENERATE_SKEINFORGE = 1;

	private static final int GENERATE_PYCAM = 2;

	// The path to the directory containing our external storage.
	private File externalStorage;

	// The path to the directory containing the game.
	private File mPath = null;

	String mFilesDirectory = null;

	private Context mContext;

	private String mArgument;

	public SkeinforgeWrapper(Context context) {
		mContext = context;
		mFilesDirectory = context.getFilesDir().getAbsolutePath();

		externalStorage = new File(Environment.getExternalStorageDirectory(),
				Main.SKEINFORGE_DIR);

		mPath = context.getFilesDir();
		mArgument = mPath.getAbsolutePath();

	}

	//
	// /**
	// * This determines if unpacking one the zip files included in
	// * the .apk is necessary. If it is, the zip file is unpacked.
	// */
	public void unpackData(final String resource, File target) {

		Log.v("python", "Extracting " + resource + " assets.");


		String[] children = target.list();		
		if(children==null || children.length == 0){
	
			recursiveDelete(target);
			target.mkdirs();
	
			AssetExtract ae = new AssetExtract(mContext);
			ae.extractTar(resource + ".mp3", target.getAbsolutePath());
		}
	}

	public void recursiveDelete(File f) {
		if (f.isDirectory()) {
			for (File r : f.listFiles()) {
				recursiveDelete(r);
			}
		}
		f.delete();
	}

	public void generateGcode(String file, String logfile, String printerModel) {
		int codeType = 1;
		switch (codeType) {
		case GENERATE_SKEINFORGE:
			generateGcodeSkeinforge(file, logfile);
			break;
		case GENERATE_PYCAM:
			generatePyCam(file, logfile);
			break;
		}
	}

	private void generatePyCam(String file, String logfile) {
		unpackData("private", mContext.getFilesDir());
		File pycamfile = new File(Environment.getExternalStorageDirectory(),
				"/Paint3d/pycam/");
		unpackData("pycam", pycamfile);

		ArrayList<String> libs = new ArrayList<String>();
		libs.add("python2.7");
		libs.add("application");

		HashMap<String, String> env = new HashMap<String, String>();
		env.put("ANDROID_PRIVATE", mFilesDirectory);
		env.put("ANDROID_ARGUMENT", mArgument);
		env.put("PYTHONOPTIMIZE", "2");
		env.put("PYTHONHOME", mFilesDirectory);
		env.put("PYTHONPATH", mArgument + ":" + mFilesDirectory + "/lib");
		env.put("SETTINGS_DIRECTORY", externalStorage.getAbsolutePath()
				+ "/settings");

		String code = "import sys, posix, os \n"
				+ "private = posix.environ['ANDROID_PRIVATE']\n"
				+ "argument = posix.environ['ANDROID_ARGUMENT']\n"
				+ "sys.path[:] = [ \n"
				+ "    private + '/lib/python27.zip', \n"
				+ "    private + '/lib/python2.7/', \n"
				+ "    private + '/lib/python2.7/lib-dynload/', \n"
				+ "    private + '/lib/python2.7/site-packages/', \n"
				+ "    argument ]\n"
				+ "import androidembed\n"
				+ "class LogFile(object):\n"
				+ "    def __init__(self,filename=''):\n"
				+ "        self.buffer = ''\n"
				+ "        self.filename = filename\n"
				+ "        if(filename!=''): \n"
				+ "            self.file = open(filename,'w')\n"
				+ "    def write(self, s):\n"
				+ "        s = self.buffer + s\n"
				+ "        lines = s.split(\"\\n\")\n"
				+ "        for l in lines[:-1]:\n"
				+ "            androidembed.log(l)\n"
				+ "            if(self.filename !=''):\n"
				+ "                self.file.write(l+'\\n')\n"
				+ "                self.file.flush()\n"
				+ "        self.buffer = lines[-1]\n"
				+ "import site; print site.getsitepackages()\n"
				+ "os.chdir('"
				+ pycamfile.getAbsolutePath()
				+ "') \n"
				+ "sys.path.append('"
				+ pycamfile.getAbsolutePath()
				+ "')\n"
				+ "import pycampy\n"
				+ "parser = pycampy.makeParser()\n"
				+ "(opts, args) = parser.parse_args(args = ['--config=test.conf','--export-gcode=output.gcode','test.stl'])\n"
				+ "exit_code = pycampy.execute(parser, opts, args, pycampy.pycam)";
		PythonRunner.executePythonCode(libs, env, code);

	}

	public void generateGcode(String file, String logfile) {
		generatePyCam(file, logfile); // default to skeinforge
	}

	public void generateGcodeSkeinforge(String file, String logfile) {
		unpackData("private", mContext.getFilesDir());
		unpackData("public", externalStorage);

		ArrayList<String> libs = new ArrayList<String>();
		libs.add("python2.7");
		libs.add("application");

		HashMap<String, String> env = new HashMap<String, String>();
		env.put("ANDROID_PRIVATE", mFilesDirectory);
		env.put("ANDROID_ARGUMENT", mArgument);
		env.put("PYTHONOPTIMIZE", "2");
		env.put("PYTHONHOME", mFilesDirectory);
		env.put("PYTHONPATH", mArgument + ":" + mFilesDirectory + "/lib");
		env.put("SETTINGS_DIRECTORY", externalStorage.getAbsolutePath()
				+ "/settings");

		String code = "import sys, posix, os \n"
				+ "private = posix.environ['ANDROID_PRIVATE']\n"
				+ "argument = posix.environ['ANDROID_ARGUMENT']\n"
				+ "sys.path[:] = [ \n"
				+ "    private + '/lib/python27.zip', \n"
				+ "    private + '/lib/python2.7/', \n"
				+ "    private + '/lib/python2.7/lib-dynload/', \n"
				+ "    private + '/lib/python2.7/site-packages/', \n"
				+ "    argument ]\n" + "import androidembed\n"
				+ "class LogFile(object):\n"
				+ "    def __init__(self,filename=''):\n"
				+ "        self.buffer = ''\n"
				+ "        self.filename = filename\n"
				+ "        if(filename!=''): \n"
				+ "            self.file = open(filename,'w')\n"
				+ "    def write(self, s):\n" + "        s = self.buffer + s\n"
				+ "        lines = s.split(\"\\n\")\n"
				+ "        for l in lines[:-1]:\n"
				+ "            androidembed.log(l)\n"
				+ "            if(self.filename !=''):\n"
				+ "                self.file.write(l+'\\n')\n"
				+ "                self.file.flush()\n"
				+ "        self.buffer = lines[-1]\n"
				+ "sys.stdout = sys.stderr = LogFile('" + logfile + "')\n"
				+ "import site; print site.getsitepackages()\n" + "os.chdir('"
				+ externalStorage.getParent() + "') \n" + "sys.path.append('"
				+ externalStorage.getAbsolutePath() + "')\n"
				+ "sys.path.append('" + externalStorage.getAbsolutePath()
				+ "/skeinforge/skeinforge_tools')\n" + "import craft\n"
				+ "print '" + SKEINFORGE_START + "'\n" + "craft.writeOutput('"
				+ file + "')";
		PythonRunner.executePythonCode(libs, env, code);

	}

	// copy skienforge from assets to sd card (can't execute from the assets
	// folder)
	private void copySkeinforge(Context context, String absolutePath)
			throws IOException {
		copyDirFile(context, absolutePath, "Skeinforge");
	}

	// recursively copy dir/files
	private void copyDirFile(Context context, String targetDir, String path)
			throws IOException {
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
			File t = new File(targetDir + '/' + path);
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

}
