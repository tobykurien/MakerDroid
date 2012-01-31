package za.co.house4hack.paint3d.spen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.util.Log;

public class ExampleUtils {
	
	public static String getFileExtension(File f) 
	{
		int idx = f.getName().indexOf(".");
		if (idx == -1)
			return "";
	    else 
	    	return f.getName().substring(idx+1);
		}
	
	public static String fileNameRemoveExtension(String fileName)
	{
		if(fileName == null)
			return null;
		
		int idx = fileName.indexOf(".");
		
		if(idx == -1)
			return fileName;
		
		else
			return fileName.substring(0, idx);		
	}
	 
	public static String stringCheck(String str) {
		StringBuilder strbuilder = new StringBuilder();
		
		int size = str.length();
		for(int i = 0; i < size; i++) {
			char curChar = str.charAt(i);
			if(curChar == '\\' || curChar == '/' || curChar == ':' || curChar == '*' || curChar == '?' || curChar == '"' 
		     || curChar == '<' || curChar == '>' || curChar == '|') {
			   strbuilder.append('_');
		   }else
			   strbuilder.append(curChar);
	   }
		return strbuilder.toString();
    }
	
	public static String getUniqueFilename(File folder, String filename, String ext) {
		if (folder == null || filename == null) return null;
		
		String curFileName;
		File curFile;

		if(filename.length() > 20){
			filename = filename.substring(0, 19);
		}
		
		filename = stringCheck(filename);
	
		int i = 1;
		do {
			curFileName = String.format("%s_%02d.%s", filename, i++, ext);
			curFile = new File(folder, curFileName);
		} while (curFile.exists());
		return curFileName;
	}
	
	public static byte[] readBytedata (String aFilename) {
		byte[] imgBuffer = null;
		
		FileInputStream fileInputStream = null;
		try {
        	File file = new File(aFilename);
        	fileInputStream = new FileInputStream(file);
        	int byteSize = (int)file.length();
        	imgBuffer = new byte[byteSize];

            if ( fileInputStream.read(imgBuffer) == -1 ) {
            	Log.e(SPenActivity.TAG, "failed to read image");
            }
            fileInputStream.close();
		} catch (FileNotFoundException e) {
        	e.printStackTrace();            
        } catch (IOException e2) {
        	e2.printStackTrace();            
        } finally {
        	if(fileInputStream != null) {
	        	try{
	        		
	        		fileInputStream.close();
	        		
	        	} catch (IOException e) {
	            	e.printStackTrace();            
	            } 
        	}
        }
		
        
        return imgBuffer;
	}

	public static boolean writeBytedata (String aFilename, byte[] imgBuffer) {

		FileOutputStream fileOutputStream = null;
		boolean result = true;
		
		try {
        	File file = new File(aFilename);
        	fileOutputStream = new FileOutputStream(file);
        	fileOutputStream.write(imgBuffer);
        	
            fileOutputStream.close();
		} catch (FileNotFoundException e) {
        	e.printStackTrace();
        	result = false;
        } catch (IOException e2) {
        	e2.printStackTrace();
        	result = false;
        } finally {
        	if(fileOutputStream != null) {
	        	try{
	        		
	        		fileOutputStream.close();
	        		
	        	} catch (IOException e) {
	            	e.printStackTrace(); 
	            	result = false;
	            } 
        	}        	
        }
		
		return result;
	}
}
