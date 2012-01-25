package za.co.house4hack.paint3d;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class Main extends Activity {
   public static final String LOG_TAG = "Paint3d";
   public static final String PAINT_DIR = "/Paint3d/";
   public static final String PAINT_EXT = ".p3d";
   
   VectorPaint vp;
   String filename = null;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
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
            final String[] files = new File(Environment.getExternalStorageDirectory() + PAINT_DIR).list(new FilenameFilter() {
               @Override
               public boolean accept(File arg0, String arg1) {
                  if (arg1.endsWith(PAINT_EXT)) return true;
                  return false;
               }
            });
                        
            // ask for filename
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Load file");
            alert.setSingleChoiceItems(files, 0, new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface arg0, int arg1) {
                  filename = files[arg1];
                  vp.loadDrawing(Environment.getExternalStorageDirectory() + PAINT_DIR + filename);
                  arg0.dismiss();
               }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int whichButton) {
                  // Canceled.
               }
            });
            alert.show();
            
            return true;
         case R.id.menu_discard:
            vp.clear();
            return true;
         case R.id.menu_preview:
            // show a progress dialog
            final ProgressDialog pd = new ProgressDialog(this);   
            pd.setMessage(getResources().getString(R.string.progress_preview));
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
               @Override
               protected void onPreExecute() {
                  super.onPreExecute();
                  pd.show();
               }
               
               @Override
               protected Void doInBackground(Void... params) {
                  vp.preview();
                  return null;
               }
               
               @Override
               protected void onPostExecute(Void result) {
                  super.onPostExecute(result);
                  pd.dismiss();
               }
            };
            task.execute(new Void[0]);
            return true;
      }
      return false;
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);

      if (resultCode == RESULT_OK) {
         // set the background image from user-selected image
         Uri targetUri = data.getData();
         ImageView iv = (ImageView) findViewById(R.id.vp_bg_image);
         iv.setImageURI(targetUri);
      }
   }

   @Override
   public void onBackPressed() {
      // Avoid accidental exits with a dialog
      new AlertDialog.Builder(this).setTitle("Exit").setMessage("Are you sure you want to exit?")
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     finish();
                  }
               }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {

                  }
               }).create().show();

   };

   // Convert a media Uri into a path
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
      if (filename == null) {
         // ask for filename
         AlertDialog.Builder alert = new AlertDialog.Builder(this);

         // alert.setTitle("Filename");
         alert.setMessage("Enter the file name");

         // Set an EditText view to get user input
         final EditText input = new EditText(this);
         alert.setView(input);

         alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
               filename = input.getText().toString() + PAINT_EXT;
               saveFile();
            }
         });

         alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
               // Canceled.
            }
         });
         alert.show();
      } else {
         saveFile();
      }
   }

   private void saveFile() {
      if (filename != null && filename.trim().length() > 0) {
         vp.saveDrawing(Environment.getExternalStorageDirectory() + PAINT_DIR + filename);
         Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
      }
   }

   public void onBackground(View v) {
      Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
      startActivityForResult(intent, 0);
   }
}