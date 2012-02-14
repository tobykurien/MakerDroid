package za.co.house4hack.paint3d.gcode;

import java.util.List;
import java.util.Map;

public class PythonRunner {

	public static void executePythonCode(List<String> libs, Map<String,String> env, String code){
		
		
		  for(String l:libs) System.loadLibrary(l);
		  for(String k:env.keySet()) nativeSetEnv(k, env.get(k));
		  runPython(code);

	}

   public static native void nativeSetEnv(String name, String value);
   public static native void runPython(String script);
	
}
