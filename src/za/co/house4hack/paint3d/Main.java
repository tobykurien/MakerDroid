package za.co.house4hack.paint3d;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

public class Main extends Activity {
   public static final String LOG_TAG = "Paint3d";
   VectorPaint vp;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // From this tutorial:
      // http://blog.jayway.com/2009/12/03/opengl-es-tutorial-for-android-part-i/
      // GLSurfaceView view = new GLSurfaceView(this);
      // view.setRenderer(new ExtrudeRenderer());
      // setContentView(view);

      setContentView(R.layout.main);
      vp = (VectorPaint) findViewById(R.id.vector_paint);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.vectorpaint, menu);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case R.id.menu_load:
            vp.loadDrawing(Environment.getExternalStorageDirectory() + "/Paint3d/" + "test.p3d");
            return true;
         case R.id.menu_discard:
            vp.clear();
            return true;
         case R.id.menu_preview:
            vp.preview();
            return true;
      }
      return false;
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      // TODO Auto-generated method stub
      super.onActivityResult(requestCode, resultCode, data);

      if (resultCode == RESULT_OK) {
         Uri targetUri = data.getData();
         // String path = getMediaPath(targetUri);
         ImageView iv = (ImageView) findViewById(R.id.vp_bg_image);
         if (iv != null) iv.setImageURI(targetUri);
         // vp.setBackgroundDrawable(iv.getDrawable());
      }
   }

   @Override
   public void onBackPressed() {
      new AlertDialog.Builder(this).setTitle("Exit").setMessage("Are you sure you want to exit?")
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     finish();
                  }
               })
               .setNegativeButton("No", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     
                  }
               })
               .create().show();

   };

   public String getMediaPath(Uri uri) {
      String[] projection = { MediaStore.Images.Media.DATA };
      Cursor cursor = managedQuery(uri, projection, null, null, null);
      int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      return cursor.getString(column_index);
   }

   public void onUndo(View v) {
      vp.undo();
   }

   public void onEtch(View v) {
      vp.etch();
   }

   public void onSave(View v) {
      vp.saveDrawing(Environment.getExternalStorageDirectory() + "/Paint3d/" + "test.p3d");
   }

   public void onBackground(View v) {
      // TODO Auto-generated method stub
      Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
      startActivityForResult(intent, 0);
   }
}