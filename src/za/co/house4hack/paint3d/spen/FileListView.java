package za.co.house4hack.paint3d.spen;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FileListView extends ListView {
	private Context mContext = null;
	private ArrayList<String> mList = new ArrayList<String>();
	private ArrayList<String> mFileList = new ArrayList<String>();
	private ArrayAdapter<String> mAdapter = null;
	
	public OnFileSelectedListener  mFileSelectedListener = null;
		
	private String mPath = "";
	private static final String EXTENSION = SPenActivity.SAVED_FILE_EXTENSION;
	
	public FileListView(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);		 
		init(context);
	}

	public FileListView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);		 
		init(context);
	}
	
	public FileListView(Context context) 
	{
		super(context);		
		init(context);
	}
	
	private void init(Context context) 
	{
		mContext = context;
		setOnItemClickListener(mOnItemClick);		
	}

	private boolean openPath(String path) 
	{
		mFileList.clear();
		
		File folder = new File(path);
		if (folder != null && !folder.exists())
			folder.mkdirs();
				
		File[] files = folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				
				String fileExtension = ExampleUtils.getFileExtension(file);
				
				if(EXTENSION.equalsIgnoreCase(fileExtension))
					return true;
				
				return false;
			}
		});		
		
		if(files == null) 
			return false;
		
		for (int i=0; i<files.length; i++)
		{
			if(files[i].isDirectory())
				continue;
			else
				mFileList.add(ExampleUtils.fileNameRemoveExtension(files[i].getName()));			
		}
		
		Collections.sort(mFileList);
		
		return true;	
	}

	private void updateAdapter()
	{
		mList.clear();
		mList.addAll(mFileList);
		
		mAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, mList);
		setAdapter(mAdapter);		
	}

	public void setPath(String path)
	{
		int length = path.length();
		if(length == 0)
			path = "/";
		else
		{
			String last = path.substring(length - 1, length);
			if(!last.matches("/"))
				path = path + '/';		
		}
		
		if(openPath(path))
		{
			mPath = path;
			updateAdapter();
		}		
	}

	public String getPath()
	{
		return mPath;		
	}

	public int getListCount()
	{
		return mList.size();
	}

	public boolean isEmpty()
	{
		return mList.isEmpty();
	}

	public void setOnFileSelectedListener(OnFileSelectedListener listner)
	{
		mFileSelectedListener = listner;
	}

	public OnFileSelectedListener getOnFileSelectedListener()
	{
		return mFileSelectedListener;
	}

	private AdapterView.OnItemClickListener mOnItemClick = 
			new	AdapterView.OnItemClickListener() 
	{
		
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1,
				int p, long id) {
			String fileName = getItemAtPosition(p).toString();
			if(mFileSelectedListener != null)
				mFileSelectedListener.onSelected(mPath, fileName + '.' + EXTENSION);		
		}
	};

}

