package za.co.house4hack.paint3d;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.SurfaceView;

public class Main extends Activity {

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // From this tutorial: http://blog.jayway.com/2009/12/03/opengl-es-tutorial-for-android-part-i/
//      GLSurfaceView view = new GLSurfaceView(this);
//      view.setRenderer(new ExtrudeRenderer());
//      setContentView(view);

      VectorPaint vp = new VectorPaint(this);
      setContentView(vp);
   }
   
}