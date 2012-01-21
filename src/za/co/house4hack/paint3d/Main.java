package za.co.house4hack.paint3d;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class Main extends Activity {
   public static final String LOG_TAG = "Paint3d";
   VectorPaint vp;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // From this tutorial: http://blog.jayway.com/2009/12/03/opengl-es-tutorial-for-android-part-i/
//      GLSurfaceView view = new GLSurfaceView(this);
//      view.setRenderer(new ExtrudeRenderer());
//      setContentView(view);

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
         case R.id.menu_discard:
            vp.clear();
            return true;
         case R.id.menu_preview:
            vp.preview();
            return true;
      }
      return false;
   }
   
   public void onUndo(View v) {
      vp.undo();
   }
   
   public void onEtch(View v) {
      vp.etch();
   }
   
   public void onSave(View v) {
      
   }
   
   public void onBackground(View v) {
      
   }
}