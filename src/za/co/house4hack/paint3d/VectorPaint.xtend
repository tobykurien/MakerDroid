package za.co.house4hack.paint3d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.samsung.sdraw.SDrawLibrary
import java.math.BigDecimal
import java.util.ArrayList
import java.util.List
import za.co.house4hack.paint3d.polytotri.ExtrudePoly
import android.content.Intent
import android.net.Uri
import java.io.File

class VectorPaint extends View {
   int CIRCLE_SPEN = 5
   int LINE_SPEN = 2
   int CIRCLE_TOUCH = 20
   int LINE_TOUCH = 10   
   
   List<Point> polygon
   Point drag
   Paint pCirc
   Paint pLine
   
   boolean isSPen = false
   int circRadius
      
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
      polygon = new ArrayList<Point>();
      
      pCirc = new Paint()
      pCirc.setARGB(255, 255, 0, 0)

      pLine = new Paint()
      pLine.setARGB(255, 0, 255, 0)
      
      isSPen = SDrawLibrary::supportedModel
      if (isSPen) {
         circRadius = CIRCLE_SPEN
         pLine.setStrokeWidth(LINE_SPEN)
      } else {
         circRadius = CIRCLE_TOUCH
         pLine.setStrokeWidth(LINE_TOUCH)
      }
            
   }
   
   override protected onDraw(Canvas canvas) {
      if (polygon.empty) {
         var pText = new Paint()
         pText.setARGB(255, 255, 255, 255)
         pText.setTextSize(40)
         canvas.drawText(helpText, 20, 40, pText)
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
         canvas.drawCircle(p2.x, p2.y, circRadius, pCirc)
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
            var bb = (circRadius * 2) + 5 // Bounding box slightly bigger than the circle
            if (Math::abs(dx.intValue) < bb && Math::abs(dy.intValue) < bb) {
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
      if (ExtrudePoly::saveToSTL(polygon, "/sdcard/paint3d.stl")) {
         var f = new File("/sdcard/paint3d.stl");
         var i = new Intent();
         i.setAction(Intent::ACTION_VIEW);
         i.setData(Uri::fromFile(f));
         context.startActivity(i);
      }
   }
   
   // undo last point
   def undo() {
      polygon.remove(polygon.size - 1)
      invalidate
   }
   
   // change brush colour and create the etching polygon
   def etch() {
      
   }
   
   def getHelpText() {
      if (isSPen) {
         getString(R$string::vp_help_spen)
      } else {
         getString(R$string::vp_help_touch)
      }
   }
   
   def getString(int resId) {
      context.resources.getString(resId)
   }
}
