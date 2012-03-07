package za.co.house4hack.paint3d;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geomgraph.Position;

import za.co.house4hack.paint3d.crop.CropOption;
import za.co.house4hack.paint3d.crop.CropOptionAdapter;
import za.co.house4hack.paint3d.spen.SPenActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class Main extends Activity {
   public static final String LOG_TAG = "MakerDroid";
   public static final String PAINT_DIR = "/MakerDroid/";
   public static final String SKEINFORGE_DIR = "/MakerDroid/Skeinforge/";
   public static final String PAINT_EXT = ".p3d";

   public static final int REQUEST_GALLERY = 3;
   public static final int REQUEST_CAMERA = 1;
   public static final int REQUEST_SPEN = 2;
   protected static final int CROP_FROM_CAMERA = 4;

   VectorPaint vp;
   String filename = null;
   private Uri mImageCaptureUri;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      vp = (VectorPaint) findViewById(R.id.vector_paint);
      if (getIntent().getData() != null) {
         String filename = getIntent().getData().getEncodedPath();

         try {
            vp.loadDrawing(filename);
         } catch (Exception e) {
            Toast.makeText(this, "Unable to load " + filename + " - " + e.getMessage(), Toast.LENGTH_LONG).show();
         }
      }

      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
      if (pref.getInt("version", 0) < 1) {
         new AlertDialog.Builder(this).setMessage(R.string.welcome_text)
                  .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                     }
                  }).create().show();
         pref.edit().putInt("version", 1).commit();
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.vectorpaint, menu);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   protected void onResume() {
      super.onResume();
      vp.loadPrefs();
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case R.id.menu_load:
            return loadDrawing();

         case R.id.menu_discard:
            vp.clear();
            filename = null;
            return true;

         case R.id.menu_preview:
            return generatePreview();

         case R.id.menu_background:
            changeBackground();
            return true;

         case R.id.menu_print:
            generateAndPrint();
            return true;

         case R.id.menu_settings:
            Intent i2 = new Intent(this, Preferences.class);
            startActivity(i2);
            return true;

         case R.id.menu_help:
            new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(R.string.help_text)
                     .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                           dialog.dismiss();
                        }
                     }).create().show();
      }
      return false;
   }

   private boolean loadDrawing() {
      final String[] files = new File(Environment.getExternalStorageDirectory() + PAINT_DIR).list(new FilenameFilter() {
         // @Override
         public boolean accept(File arg0, String arg1) {
            if (arg1.endsWith(PAINT_EXT)) return true;
            return false;
         }
      });

      // ask for filename
      AlertDialog.Builder alert = new AlertDialog.Builder(this);

      alert.setTitle("Load file");
      alert.setSingleChoiceItems(files, 0, new DialogInterface.OnClickListener() {
         // @Override
         public void onClick(DialogInterface arg0, int arg1) {
            filename = files[arg1];
            try {
               vp.loadDrawing(Environment.getExternalStorageDirectory() + PAINT_DIR + filename);
            } catch (IOException e) {
               Toast.makeText(Main.this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            arg0.dismiss();
         }
      });

      alert.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int whichButton) {
            // Canceled.
         }
      });
      alert.show();

      return true;
   }

   public String getFilenameNoExt() {
      if (filename == null) return "untitled";

      String retVal = filename.substring(0, filename.length() - PAINT_EXT.length());
      return retVal;
   }

   private boolean generatePreview() {
      // check if we have an STL viewer app
      final File f = new File(getSdDir() + "/" + getFilenameNoExt() + ".stl");
      Intent i = new Intent();
      i.setAction(Intent.ACTION_VIEW);
      i.setDataAndType(Uri.fromFile(f), "");
      List l = getPackageManager().queryIntentActivities(i, 0);
      if (l.isEmpty()) {
         // ask user to install STL viewer
         new AlertDialog.Builder(this).setTitle(R.string.title_stl_viewer).setMessage(R.string.msg_stl_viewer)
                  .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("http://www.freestlview.com/"));
                        //i.setData(Uri.parse("market://search?q=pname:moduleWorks.STLView"));
                        startActivity(i);
                     }
                  }).setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                     }
                  }).create().show();
         return true;
      }

      if (vp.drawingEmpty()) {
         Toast.makeText(this, R.string.err_drawing_empty, Toast.LENGTH_LONG).show();
         return true;
      }

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
            try {
               vp.preview(f.getAbsolutePath());
            } catch (final Exception e) {
               Log.e(LOG_TAG, "Error generating STL", e);
               runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                     Toast.makeText(Main.this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                  }
               });
            }
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

   public static String getSdDir() {
      String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath() + PAINT_DIR;
      File f = new File(sdDir);
      if (!f.exists()) f.mkdirs();
      return sdDir;
   }

   private void changeBackground() {
      // pick a background image
      new AlertDialog.Builder(this).setTitle(R.string.title_background_image)
               .setSingleChoiceItems(R.array.image_sources, 0, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface arg0, int arg1) {
                     arg0.dismiss();
                     switch (arg1) {
                        case 0:
                           // take pic
                           captureImageIntent();
                           break;
                        case 1:
                           // freehand drawing
                           if (vp.isSPen) {
                              Intent intent = new Intent(Main.this, SPenActivity.class);
                              startActivityForResult(intent, REQUEST_SPEN);
                           } else {
                              Toast.makeText(Main.this, R.string.err_no_spen, Toast.LENGTH_LONG).show();
                           }
                           break;
                        case 2:
                           // gallery
                           Intent intent2 = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                           startActivityForResult(intent2, REQUEST_GALLERY);
                           break;
                        case 3:
                           // remove background
                           ImageView iv = (ImageView) findViewById(R.id.vp_bg_image);
                           iv.setImageBitmap(null);
                           break;
                     }
                  }
               }).create().show();
   }

   protected void captureImageIntent() {
      // see
      // http://stackoverflow.com/questions/1910608/android-action-image-capture-intent
      Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
      mImageCaptureUri = Uri.fromFile(new File(getSdDir(), "bg_crop_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));

      i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
      startActivityForResult(i, REQUEST_CAMERA);
   }

   private void generateAndPrint() {
      if (filename == null) {
         Toast.makeText(this, R.string.err_save_first, Toast.LENGTH_LONG).show();
         return;
      }
      saveFile(false);

      if (vp.drawingEmpty()) {
         Toast.makeText(this, R.string.err_drawing_empty, Toast.LENGTH_LONG).show();
         return;
      }

      new AlertDialog.Builder(this).setTitle(R.string.title_print).setMessage(R.string.msg_print)
               .setPositiveButton(R.string.btn_continue, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     dialog.dismiss();
                     // show a progress dialog
                     final ProgressDialog pd = new ProgressDialog(Main.this);
                     pd.setMessage(getResources().getString(R.string.progress_print));
                     AsyncTask<Void, String, String> task = new AsyncTask<Void, String, String>() {
                        @Override
                        protected void onPreExecute() {
                           super.onPreExecute();
                           pd.show();
                        }

                        @Override
                        protected String doInBackground(Void... params) {
                           String sdDir = getSdDir();
                           SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(Main.this);
                           String printerModel = pref.getString("printer", "bfb_rapman_31_dual");
                           String file = sdDir + getFilenameNoExt() + ".stl";

                           final File logFile = new File(file + ".log");
                           Thread tail = new Thread() {
                              boolean stop = false;
                              
                              @Override
                              public void run() {
                                 while (!logFile.exists());
                                 try {
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    FileInputStream is = new FileInputStream(logFile);
                                    byte[] buf = new byte[1024];
                                    int len = 0;
                                    while (!stop) {
                                       if (is.available() > 0) {
                                          len = is.read(buf);
                                          out.write(buf, 0, len);
                                          publishProgress(out.toString());
                                       }
                                    }
                                 } catch (IOException e) {
                                    Log.d(Main.LOG_TAG, "ERROR tailing log file: " + e.getMessage());
                                 }
                              }
                              
                              @Override
                              public void interrupt() {
                                 stop = true;
                                 super.interrupt();
                              }
                           };

                           try {
                              tail.start();
                              vp.print(file, printerModel);
                           } catch (final Exception e) {
                              Log.e(LOG_TAG, "Error generating STL", e);
                              runOnUiThread(new Runnable() {
                                 @Override
                                 public void run() {
                                    Toast.makeText(Main.this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                 }
                              });
                           } finally {
                              tail.stop();
                           }

                           // check if SD card is plugged in via USB (works for
                           // Samsung)
                           String result = sdDir + getFilenameNoExt() + "_export.bfb";
                           File src = new File(result);
                           File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Paint3d");
                           if (f.exists()) {
                              // copy the generated print to the SD card
                              try {
                                 FileInputStream is = new FileInputStream(src);
                                 FileOutputStream os = new FileOutputStream(f);
                                 try {
                                    byte[] buf = new byte[1024];
                                    int len = 0;
                                    while ((len = is.read(buf)) > 0) {
                                       os.write(buf, 0, len);
                                    }
                                    result = f.getAbsolutePath();
                                 } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                 } finally {
                                    is.close();
                                    os.close();
                                 }
                              } catch (Exception e) {
                                 // ignore errors
                              }
                           }
                           return result;
                        }
                        
                        @Override
                        protected void onProgressUpdate(String... values) {
                           super.onProgressUpdate(values);
                           pd.setTitle("Progress log");
                           int len = values[0].length();
                           int start = len - 255;
                           if (start < 0) start = 0;
                           pd.setMessage(values[0].substring(start, len ));
                        }

                        @Override
                        protected void onPostExecute(String result) {
                           super.onPostExecute(result);
                           pd.dismiss();

                           new AlertDialog.Builder(Main.this).setMessage(getString(R.string.print_text) + result)
                                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialog, int which) {
                                          dialog.dismiss();
                                       }
                                    }).create().show();
                        }
                     };
                     task.execute(new Void[0]);
                  }
               }).setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     dialog.dismiss();
                  }
               }).create().show();
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      ImageView iv = (ImageView) findViewById(R.id.vp_bg_image);
      if (resultCode == RESULT_OK) {
         iv.setVisibility(View.VISIBLE);
      }

      if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK) {
         // set the background image from user-selected image
         Uri targetUri = data.getData();
         iv.setImageURI(targetUri);
         iv.setVisibility(View.VISIBLE);
      } else if (requestCode == REQUEST_SPEN && resultCode == RESULT_OK) {
         // freehand background
         byte[] bd = data.getByteArrayExtra(Intent.EXTRA_STREAM);
         Bitmap bm = new BitmapFactory().decodeByteArray(bd, 0, bd.length);
         iv.setImageBitmap(bm);
         iv.setBackgroundColor(R.color.freehand_bg);
      } else if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
         // pic from camera
         // Log.d(LOG_TAG, "In REQUEST_CAMERA");
         doCrop();
      } else if (requestCode == CROP_FROM_CAMERA && resultCode == RESULT_OK) {
         // pic from camera
         Bundle extras = data.getExtras();

         if (extras != null) {
            Bitmap photo = extras.getParcelable("data");

            File folder = Main.this.getCacheDir();
            try {
               String filename = folder.getPath() + "/camera.png";
               FileOutputStream out = new FileOutputStream(filename);
               photo.compress(Bitmap.CompressFormat.PNG, 90, out);
               out.close();
            } catch (Exception e) {
               e.printStackTrace();
            }

            iv.setImageBitmap(photo);
         }

         File f = new File(mImageCaptureUri.getPath());
         if (f.exists()) f.delete();

      }

      super.onActivityResult(requestCode, resultCode, data);
   }

   @Override
   public void onBackPressed() {
      // Avoid accidental exits with a dialog
      new AlertDialog.Builder(this).setTitle("Exit").setMessage("Are you sure you want to exit?")
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                  // @Override
                  public void onClick(DialogInterface dialog, int which) {
                     finish();
                  }
               }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                  // @Override
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

   public void onLayer(View v) {
      vp.layer();
   }

   public void onNewPoly(View v) {
      vp.newPoly();
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

         alert.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
               filename = input.getText().toString();
               if (filename.trim().length() == 0 || filename.startsWith(".")) {
                  filename = null;
                  Toast.makeText(Main.this, R.string.err_invalid_filename, Toast.LENGTH_LONG).show();
                  onSave(null);
               } else {
                  filename += PAINT_EXT;
                  saveFile(true);
               }
            }
         });

         alert.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
               // Canceled.
            }
         });
         alert.show();
      } else {
         saveFile(true);
      }
   }

   private void saveFile(boolean showToast) {
      if (filename != null && filename.trim().length() > 0) {
         try {
            vp.saveDrawing(Environment.getExternalStorageDirectory() + PAINT_DIR + filename);
            
            if (showToast) {
               runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                     Toast.makeText(Main.this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
                  }
               });
            }
         } catch (final Exception e) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                  Toast.makeText(Main.this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
               }
            });
         }
      } else {
         if (showToast) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                  Toast.makeText(Main.this, R.string.err_invalid_filename, Toast.LENGTH_LONG).show();                           
               }
            });
         }
      }
   }

   public void onBackground(View v) {
      // toggle background
      ImageView iv = (ImageView) findViewById(R.id.vp_bg_image);
      if (iv.isShown()) {
         iv.setVisibility(View.INVISIBLE);
      } else {
         iv.setVisibility(View.VISIBLE);
      }
   }

   public void onDeletePoly(View v) {
      vp.deletePoly();
   }

   private void doCrop() {
      final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();

      Intent intent = new Intent("com.android.camera.action.CROP");
      intent.setType("image/*");

      List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);

      int size = list.size();

      if (size == 0) {
         Toast.makeText(this, "Can not find image crop app", Toast.LENGTH_SHORT).show();

         return;
      } else {
         File folder = Main.this.getCacheDir();
         intent.setData(mImageCaptureUri);
         // intent.setData(
         // android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

         intent.putExtra("outputX", 400);
         intent.putExtra("outputY", 300);
         // intent.putExtra("aspectX", 4);
         // intent.putExtra("aspectY", 3);
         intent.putExtra("scale", true);
         intent.putExtra("return-data", true);

         if (size == 1) {
            Intent i = new Intent(intent);
            ResolveInfo res = list.get(0);

            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

            startActivityForResult(i, CROP_FROM_CAMERA);
         } else {
            for (ResolveInfo res : list) {
               final CropOption co = new CropOption();

               co.title = getPackageManager().getApplicationLabel(res.activityInfo.applicationInfo);
               co.icon = getPackageManager().getApplicationIcon(res.activityInfo.applicationInfo);
               co.appIntent = new Intent(intent);

               co.appIntent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

               cropOptions.add(co);
            }

            CropOptionAdapter adapter = new CropOptionAdapter(getApplicationContext(), cropOptions);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose Crop App");
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int item) {
                  startActivityForResult(cropOptions.get(item).appIntent, CROP_FROM_CAMERA);
               }
            });

            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
               @Override
               public void onCancel(DialogInterface dialog) {

                  if (mImageCaptureUri != null) {
                     getContentResolver().delete(mImageCaptureUri, null, null);
                     mImageCaptureUri = null;
                  }
               }
            });

            AlertDialog alert = builder.create();

            alert.show();
         }
      }
   }

}