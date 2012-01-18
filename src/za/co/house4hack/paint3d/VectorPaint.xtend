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
import android.util.AttributeSet

class VectorPaint extends View {
   List<Point> polygon
   Point drag
   Paint pCirc
   Paint pLine
      
   new(Context context) {
      super(context)
      init
   }
   
   new(Context arg0, AttributeSet arg1) {
      super(arg0, arg1)
      init
   }
   
   new(Context arg0, AttributeSet arg1, int arg2) {
      super(arg0, arg1, arg2)
      init
   }
   
   def init() {
      polygon = new ArrayList();
      
      pCirc = new Paint()
      pCirc.setARGB(255, 255, 0, 0)

      pLine = new Paint()
      pLine.setARGB(255, 0, 255, 0)
      pLine.setStrokeWidth(10)      
   }
   
   override protected onDraw(Canvas canvas) {
      if (polygon.empty) {
         var pText = new Paint()
         pText.setARGB(255, 255, 255, 255)
         pText.setTextSize(40)
         canvas.drawText("Tap to start drawing", 20, 40, pText)
         return;         
      }
      
      // draw points where canvas was touched
      var Point pp = null
      for (Point p : polygon) {
         if (pp != null) {
            canvas.drawLine(pp.x, pp.y, p.x, p.y, pLine)
         }
         pp = p;
      }

      // complete the polygon      
      var p = polygon.get(0)
      canvas.drawLine(pp.x, pp.y, p.x, p.y, pLine)      

      // draw the circles afterwards to place over the line
      for (Point p2 : polygon) {
         canvas.drawCircle(p2.x, p2.y, 20, pCirc)
      }
         
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
            if (Math::abs(dx.intValue) < 30 && Math::abs(dy.intValue) < 30) {
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
 
   // clear the canvas and start a new drawing
   def clear() {
      polygon.clear
      drag = null
      invalidate
   }  
   
   // save the shape and preview in 3D using STL viewer
   def preview() {
      // preview the drawing by converting polygon to 3D and then save as STL file
   }
   
   // undo last point
   def undo() {
      polygon.remove(polygon.size - 1)
      invalidate
   }
   
   // change brush colour and create the etching polygon
   def etch() {
      
   }
}
