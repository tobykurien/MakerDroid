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
import java.math.BigDecimal

class VectorPaint extends View {
   List<Point> polygon
   Point drag
      
   new(Context context) {
      super(context)
      polygon = new ArrayList();
   }
   
   override protected onDraw(Canvas canvas) {
      if (polygon.empty) {
         var pText = new Paint()
         pText.setARGB(255, 0, 0, 255)
         pText.setTextSize(20)
         canvas.drawText("Tap to start drawing", 20, 20, pText)
         return;         
      }
      
      // draw points where canvas was touched
      var pCirc = new Paint()
      pCirc.setARGB(255, 255, 0, 0)
      pCirc.setStrokeWidth(1)

      var pLine = new Paint()
      pLine.setARGB(255, 0, 255, 0)
      pCirc.setStrokeWidth(5)
      
      var Point pp = null
      for (Point p : polygon) {
         if (pp != null) canvas.drawLine(pp.x, pp.y, p.x, p.y, pLine)
         canvas.drawCircle(p.x, p.y, 20, pCirc)
         pp = p;
      }

      // complete the polygon      
      var p = polygon.get(0)
      canvas.drawLine(pp.x, pp.y, p.x, p.y, pLine)      
         
      super.onDraw(canvas)
   }
   
   override onTouchEvent(MotionEvent event) {
      var handled = false;
      
      if (event.action == MotionEvent::ACTION_DOWN) {
         drag = null;
         
         // surface was clicked
         // check if the click was on top of an existing point
         for (Point p : polygon) {
            // check if circle was tapped (using bounding box around point)
            var dx = new BigDecimal(p.x) - new BigDecimal(event.x)
            var dy = new BigDecimal(p.y) - new BigDecimal(event.y)
            if (Math::abs(dx.intValue) < 20 && Math::abs(dy.intValue) < 20) {
              // clicked existing point, allow dragging it 
              drag = p
              // TODO - break here
            }
         }

         if (drag == null) {
            // add a point here
            polygon.add(new Point(event.x, event.y))
         }    
         invalidate 
         handled = true             
      }
      
      if (event.action == MotionEvent::ACTION_MOVE) {
         if (drag != null) {
            drag.x = event.x
            drag.y = event.y
         }
         invalidate              
         handled = true             
      }
      
      if (event.action == MotionEvent::ACTION_UP) {
         if (drag != null) {
               Log::d("vectorpaint",  "drag complete")
            drag.x = event.x
            drag.y = event.y
            drag = null // the point is up to date
         }
         invalidate              
         handled = true             
      }
      
    handled || super.onTouchEvent(event)
   }
 
   def clear() {
      polygon.clear
      drag = null
   }  
   
   def preview() {
      // preview the drawing by converting polygon to 3D and then save as STL file
   }
}
