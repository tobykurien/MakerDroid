package za.co.house4hack.paint3d;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import za.co.house4hack.paint3d.crop.CropOption;
import za.co.house4hack.paint3d.crop.CropOptionAdapter;
import za.co.house4hack.paint3d.spen.SPenActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
	public static final String LOG_TAG = "Paint3d";
	public static final String PAINT_DIR = "/Paint3d/";
	public static final String SKEINFORGE_DIR = "/Paint3d/Skeinforge/";
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
			if (!vp.loadDrawing(filename)) {
				Toast.makeText(this, "Unable to load " + filename,
						Toast.LENGTH_LONG).show();
			}
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
			final String[] files = new File(
					Environment.getExternalStorageDirectory() + PAINT_DIR)
					.list(new FilenameFilter() {
						// @Override
						public boolean accept(File arg0, String arg1) {
							if (arg1.endsWith(PAINT_EXT))
								return true;
							return false;
						}
					});

			// ask for filename
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Load file");
			alert.setSingleChoiceItems(files, 0,
					new DialogInterface.OnClickListener() {
						// @Override
						public void onClick(DialogInterface arg0, int arg1) {
							filename = files[arg1];
							vp.loadDrawing(Environment
									.getExternalStorageDirectory()
									+ PAINT_DIR
									+ filename);
							arg0.dismiss();
						}
					});

			alert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Canceled.
						}
					});
			alert.show();

			return true;

		case R.id.menu_discard:
			vp.clear();
			filename = null;
			return true;

		case R.id.menu_preview:
		   // check if we have n STL viewer app
	      String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath();
         File f = new File(sdDir + "/paint3d.stl");
         Intent i = new Intent();
         i.setAction(Intent.ACTION_VIEW);
         i.setDataAndType(Uri.fromFile(f), "");
         List l = getPackageManager().queryIntentActivities(i, 0);
         if (l.isEmpty()) {
            // ask user to install STL viewer
            new AlertDialog.Builder(this)
               .setTitle(R.string.title_stl_viewer)
               .setMessage(R.string.msg_stl_viewer)
               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {                  
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     dialog.dismiss();
                     Intent i = new Intent(Intent.ACTION_VIEW);
                     i.setData(Uri.parse("market://search?q=pname:moduleWorks.STLView"));
                     startActivity(i);
                  }
               })
               .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     dialog.dismiss();
                  }
               })
               .create().show();
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

		case R.id.menu_background:
			// pick a background image
			new AlertDialog.Builder(this)
					.setTitle(R.string.title_background_image)
					.setSingleChoiceItems(R.array.image_sources, 0,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									arg0.dismiss();
									switch (arg1) {
									case 0:
										// take pic
										captureImageIntent();
										break;
									case 1:
										// freehand drawing
										if (vp.isSPen) {
											Intent intent = new Intent(
													Main.this,
													SPenActivity.class);
											startActivityForResult(intent,
													REQUEST_SPEN);
										} else {
											Toast.makeText(Main.this,
													R.string.err_no_spen,
													Toast.LENGTH_LONG).show();
										}
										break;
									case 2:
										// gallery
										Intent intent2 = new Intent(
												Intent.ACTION_PICK,
												android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
										startActivityForResult(intent2,
												REQUEST_GALLERY);
										break;
									case 3:
										// remove background
										ImageView iv = (ImageView) findViewById(R.id.vp_bg_image);
										iv.setImageBitmap(null);
										break;
									}
								}
							}).create().show();
			return true;

		case R.id.menu_print:
			generateAndPrint();
			return true;

		case R.id.menu_settings:
			Intent i2 = new Intent(this, Preferences.class);
			startActivity(i2);
			return true;

		case R.id.menu_help:
			new AlertDialog.Builder(this)
					.setTitle(R.string.app_name)
					.setMessage(R.string.help_text)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).create().show();
		}
		return false;
	}

	protected void captureImageIntent() {
		// see
		// http://stackoverflow.com/questions/1910608/android-action-image-capture-intent
		Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
				   "tmp_avatar_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));

        i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
		startActivityForResult(i, REQUEST_CAMERA);
	}

	private void generateAndPrint() {
		// show a progress dialog
		final ProgressDialog pd = new ProgressDialog(this);
		pd.setMessage(getResources().getString(R.string.progress_print));
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				pd.show();
			}

			@Override
			protected Void doInBackground(Void... params) {
				vp.print();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				pd.dismiss();
			}
		};
		task.execute(new Void[0]);
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
			Log.d(LOG_TAG,"In REQUEST_CAMERA");
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
		new AlertDialog.Builder(this)
				.setTitle("Exit")
				.setMessage("Are you sure you want to exit?")
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							// @Override
							public void onClick(DialogInterface dialog,
									int which) {
								finish();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					// @Override
					public void onClick(DialogInterface dialog, int which) {

					}
				}).create().show();

	};

	// Convert a media Uri into a path
	public String getMediaPath(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
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

			alert.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							filename = input.getText().toString() + PAINT_EXT;
							saveFile();
						}
					});

			alert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
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
			vp.saveDrawing(Environment.getExternalStorageDirectory()
					+ PAINT_DIR + filename);
			Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
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

	private void doCrop() {
		final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();

		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setType("image/*");

		List<ResolveInfo> list = getPackageManager().queryIntentActivities(
				intent, 0);

		int size = list.size();

		if (size == 0) {
			Toast.makeText(this, "Can not find image crop app",
					Toast.LENGTH_SHORT).show();

			return;
		} else {
			File folder = Main.this.getCacheDir();
			intent.setData(mImageCaptureUri);
			//intent.setData( android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

			intent.putExtra("outputX", 400);
			intent.putExtra("outputY", 300);
			//intent.putExtra("aspectX", 4);
			//intent.putExtra("aspectY", 3);
			intent.putExtra("scale", true);
			intent.putExtra("return-data", true);

			if (size == 1) {
				Intent i = new Intent(intent);
				ResolveInfo res = list.get(0);

				i.setComponent(new ComponentName(res.activityInfo.packageName,
						res.activityInfo.name));

				 startActivityForResult(i, CROP_FROM_CAMERA);
			} else {
				for (ResolveInfo res : list) {
					final CropOption co = new CropOption();

					co.title = getPackageManager().getApplicationLabel(
							res.activityInfo.applicationInfo);
					co.icon = getPackageManager().getApplicationIcon(
							res.activityInfo.applicationInfo);
					co.appIntent = new Intent(intent);

					co.appIntent
							.setComponent(new ComponentName(
									res.activityInfo.packageName,
									res.activityInfo.name));

					cropOptions.add(co);
				}

				CropOptionAdapter adapter = new CropOptionAdapter(
						getApplicationContext(), cropOptions);

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Choose Crop App");
				builder.setAdapter(adapter,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								startActivityForResult(
										cropOptions.get(item).appIntent,
										CROP_FROM_CAMERA);
							}
						});

				builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

		                if (mImageCaptureUri != null ) {
		                    getContentResolver().delete(mImageCaptureUri, null, null );
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