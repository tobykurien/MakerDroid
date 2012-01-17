package za.co.house4hack.paint3d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.List
import java.util.ArrayList
import android.util.Log
import android.view.View

class VectorPaint extends View {
   List<Point> polygon
   Point drag
      
   new(Context context) {
      super(context)
      polygon = new ArrayList();
   }
   
   override protected onDraw(Canvas canvas) {
      if (polygon.empty) return;
      
      // draw points where canvas was touched
      var paint = new Paint()
      paint.setARGB(255, 255, 0, 0)
      paint.setStrokeWidth(2)
      
      var Point pp = null
      for (Point p : polygon) {
         if (pp != null) canvas.drawLine(pp.x, pp.y, p.x, p.y, paint)
         canvas.drawCircle(p.x, p.y, 2, paint)
         pp = p;
      }
         
      super.onDraw(canvas)
   }
   
   override onTouchEvent(MotionEvent event) {
      if (event.action == MotionEvent::ACTION_DOWN) {
         // surface was clicked
         Log::d("vectorpaint",  "screen touched")
         // check if the click was on top of an existing point
         for (Point p : polygon) {
            // TODO - add a bounding box so click doesn't need to be exact
            if (p.x == event.x && p.y == event.y) {
              // clicked existing point, allow dragging it 
              drag = p
              // TODO - break here
            }
         }

         if (drag == null) {
            // add a point here
            polygon.add(new Point(event.x, event.y))
            invalidate
         }                  
         Log::d("vectorpaint",  "polygon size " + polygon.size)
      } else if (event.action == MotionEvent::ACTION_MOVE) {
         if (drag != null) {
            drag.x = event.x
            drag.y = event.y
         }
      } else if (event.action == MotionEvent::ACTION_UP) {
         if (drag != null) {
            drag = null // the point is up to date
            drag.x = event.x
            drag.y = event.y
         }
      }

      
      
      super.onTouchEvent(event)
   }
   
}
