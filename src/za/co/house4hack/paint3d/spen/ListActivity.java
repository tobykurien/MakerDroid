package za.co.house4hack.paint3d.spen;

import za.co.house4hack.paint3d.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;


public class ListActivity extends Activity {
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.spen_list);

		FileListView fListView = (FileListView) findViewById(R.id.fileListView);

		fListView.setPath(SPenActivity.DEFAULT_APP_IMAGEDATA_DIRECTORY);

		fListView.setOnFileSelectedListener(mFileSelectedListener);
		
		TextView statusTextView = (TextView) findViewById(R.id.statusTitle);
		String status = null;
		
		if(fListView.isEmpty())
			status = "File not Found";		
		else
		{
			int count = fListView.getListCount();
			
			if(count == 1)
				status = "Total (1) File";
			else
				status = "Total (" + fListView.getListCount() + ") Files";			
		}
		
		statusTextView.setText(status);
		
		fListView.setFocusable(true);
		fListView.setFocusableInTouchMode(true);		
	}
	
	private OnFileSelectedListener mFileSelectedListener = new OnFileSelectedListener() {
		
		@Override
		public void onSelected(String path, String fileName) {
			Intent intent = getIntent();			
			intent.putExtra(SPenActivity.EXTRA_IMAGE_PATH, path); 
			intent.putExtra(SPenActivity.EXTRA_IMAGE_NAME, fileName);
			setResult(RESULT_OK, intent);	
			finish();
		}
	};
}
