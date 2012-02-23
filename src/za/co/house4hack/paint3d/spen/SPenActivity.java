package za.co.house4hack.paint3d.spen;

import java.io.File;

import za.co.house4hack.paint3d.Main;
import za.co.house4hack.paint3d.R;
import za.co.house4hack.paint3d.R.id;
import za.co.house4hack.paint3d.R.layout;
import za.co.house4hack.paint3d.R.menu;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.samsung.sdraw.AbstractSettingView;
import com.samsung.sdraw.AbstractSettingView.SettingChangeListener;
import com.samsung.sdraw.CanvasView;
import com.samsung.sdraw.PenSettingInfo;
import com.samsung.sdraw.SettingView;

public class SPenActivity extends Activity {
   public static final String TAG = "Example1";

   public static final String DEFAULT_APP_DIRECTORY = Main.PAINT_DIR;
   public static final String DEFAULT_APP_IMAGEDATA_DIRECTORY = DEFAULT_APP_DIRECTORY + "/image";

   public static final String EXTRA_IMAGE_PATH = "path";
   public static final String EXTRA_IMAGE_NAME = "filename";

   public static final String SAVED_FILE_EXTENSION = "png";

   private CanvasView mCanvasView;
   private Button mPenBtn;
   private Button mEraserBtn;
   private Button mUndoBtn;
   private Button mRedoBtn;
   private SettingView mSettingView;

   private File mFolder = null;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.spen_main);

      mPenBtn = (Button) findViewById(R.id.settingBtn);
      mPenBtn.setOnClickListener(mBtnClickListener);
      mEraserBtn = (Button) findViewById(R.id.eraseBtn);
      mEraserBtn.setOnClickListener(mBtnClickListener);
      mUndoBtn = (Button) findViewById(R.id.undoBtn);
      mUndoBtn.setOnClickListener(undoNredoBtnClickListener);
      mRedoBtn = (Button) findViewById(R.id.redoBtn);
      mRedoBtn.setOnClickListener(undoNredoBtnClickListener);

      mCanvasView = (CanvasView) findViewById(R.id.canvas_view);
      mSettingView = (SettingView) findViewById(R.id.setting_view);

      mCanvasView.setSettingView(mSettingView);

      mCanvasView.setOnHistoryChangeListener(historyChangeListener);

      mSettingView.setOnSettingChangeListener(settingChangeListener);

      mFolder = new File(Main.getSdDir() + DEFAULT_APP_IMAGEDATA_DIRECTORY);

      mPenBtn.setSelected(true);
      mUndoBtn.setEnabled(false);
      mRedoBtn.setEnabled(false);

      mCanvasView.getPenSettingInfo().setPenColor(PenSettingInfo.PEN_TYPE_SOLID, 0xffffff);
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);

      if (resultCode == RESULT_OK) {
         Bundle bundle = data.getExtras();
         String mImagePath = bundle.getString(EXTRA_IMAGE_PATH);
         String mFileName = bundle.getString(EXTRA_IMAGE_NAME);

         if (loadCanvas(mFileName)) Log.d(TAG, "LOAD succeed");
         else
            Log.d(TAG, "LOAD failed");
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.spen_menu, menu);
      return super.onCreateOptionsMenu(menu);

   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      super.onOptionsItemSelected(item);

      switch (item.getItemId()) {
         case R.id.menu_done:
            drawingDone();
            return true;
            /*
             * case R.id.options_save:
             * Log.d(TAG, "SAVE");
             * if(saveCanvas())
             * Log.d(TAG, "SAVE succeed");
             * else
             * Log.d(TAG, "SAVE failed");
             * return true;
             * 
             * case R.id.options_load:
             * Log.d(TAG, "LOAD");
             * startFileListActivity();
             * return true;
             */
      }

      return false;
   }

   public boolean saveCanvas() {
      byte[] buffer = mCanvasView.getData();

      if (buffer == null) return false;

      String savePath = mFolder.getPath() + '/' + ExampleUtils.getUniqueFilename(mFolder, "image", SAVED_FILE_EXTENSION);
      Log.d(TAG, "Save Path = " + savePath);

      if (ExampleUtils.writeBytedata(savePath, buffer)) return true;
      else
         return false;
   }

   // send the drawing back to the main activity for use as background
   public boolean drawingDone() {
      byte[] buffer = mCanvasView.getData();
      
      Intent i = new Intent();
      i.setType("image/bitmap");
      i.putExtra(Intent.EXTRA_STREAM, buffer);
      setResult(RESULT_OK, i);
      finish();
      
      return true;
   }   
   
   public boolean loadCanvas(String fileName) {

      String loadPath = mFolder.getPath() + '/' + fileName;

      Log.d(TAG, "Load Path = " + loadPath);

      byte[] buffer = ExampleUtils.readBytedata(loadPath);

      if (buffer == null) return false;

      mCanvasView.setData(buffer);

      return true;
   }

   public void startFileListActivity() {
      Intent intent = new Intent(getApplicationContext(), ListActivity.class);
      startActivityForResult(intent, 0);
   }

   private OnClickListener undoNredoBtnClickListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
         if (v == mUndoBtn) {
            mCanvasView.undo();
         } else if (v == mRedoBtn) {
            mCanvasView.redo();
         }

         mUndoBtn.setEnabled(mCanvasView.isUndoable());
         mRedoBtn.setEnabled(mCanvasView.isRedoable());
      }
   };

   OnClickListener mBtnClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         if (v.getId() == mPenBtn.getId()) {

            mCanvasView.changeModeTo(CanvasView.PEN_MODE);

            if (mPenBtn.isSelected()) {
               if (mSettingView.isShown(AbstractSettingView.PEN_SETTING_VIEW)) mSettingView.closeView();
               else
                  mSettingView.showView(AbstractSettingView.PEN_SETTING_VIEW);

            } else {
               mPenBtn.setSelected(true);
               mEraserBtn.setSelected(false);

               if (mSettingView.isShown(AbstractSettingView.ERASER_SETTING_VIEW)) mSettingView.closeView();
            }
         } else if (v.getId() == mEraserBtn.getId()) {
            mCanvasView.changeModeTo(CanvasView.ERASER_MODE);

            if (mEraserBtn.isSelected()) {

               if (mSettingView.isShown(AbstractSettingView.ERASER_SETTING_VIEW)) mSettingView.closeView();
               else
                  mSettingView.showView(AbstractSettingView.ERASER_SETTING_VIEW);
            } else {
               mEraserBtn.setSelected(true);
               mPenBtn.setSelected(false);

               if (mSettingView.isShown(AbstractSettingView.PEN_SETTING_VIEW)) mSettingView.closeView();

            }
         }
      }
   };

   private AbstractSettingView.SettingChangeListener settingChangeListener = new SettingChangeListener() {

      //
      @Override
      public void onPenTypeChanged(int type) {

         if (type == SettingView.PEN_TYPE_BRUSH) Log.d(TAG, "Pen Type = Brush");
         else if (type == SettingView.PEN_TYPE_PENCIL) Log.d(TAG, "Pen Type = Pencil");
         else if (type == SettingView.PEN_TYPE_SOLID) Log.d(TAG, "Pen Type = Solid");
         else if (type == SettingView.PEN_TYPE_HILIGHTER) Log.d(TAG, "Pen Type = Hilighter");
      }

      @Override
      public void onColorChanged(int rgb) {

         Log.d(TAG, "Pen RGB Color = (" + Color.red(rgb) + ", " + Color.green(rgb) + ", " + Color.blue(rgb) + ')');

      }

      @Override
      public void onClearAll() {

         Log.d(TAG, "Canvas Clear All!");
      }

      @Override
      public void onEraserWidthChanged(int width) {

         Log.d(TAG, "Eraser Width = " + width);
      }

      @Override
      public void onPenAlphaChanged(int alpha) {

         Log.d(TAG, "Pen Alpha Color = " + alpha);
      }

      @Override
      public void onPenWidthChanged(int width) {

         Log.d(TAG, "Pen Width = " + width);
      }
   };

   private CanvasView.OnHistoryChangeListener historyChangeListener = new CanvasView.OnHistoryChangeListener() {

      @Override
      public void onHistoryChanged(boolean bUndoable, boolean bRedoable) {

         mUndoBtn.setEnabled(bUndoable);
         mRedoBtn.setEnabled(bRedoable);

      }
   };
}
