package za.co.house4hack.paint3d

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceView
import java.util.List

class VectorPaint extends SurfaceView {
   List<Point> polygon
   Point drag
      
   new(Context context) {
      super(context)
   }
   
   override protected onDraw(Canvas canvas) {
      // draw points where canvas was touched
   
     
      
      super.onDraw(canvas)
   }
   
   override onTouchEvent(MotionEvent event) {
      if (event.action == MotionEvent::ACTION_DOWN) {
         // surface was clicked
         
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
         }                  
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
