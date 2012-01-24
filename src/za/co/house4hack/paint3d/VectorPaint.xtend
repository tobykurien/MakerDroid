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
import android.os.Environment
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.FileInputStream

class VectorPaint extends View {
   int CIRCLE_SPEN = 5
   int LINE_SPEN = 2
   int Y_CORRECT = 10
   int X_CORRECT = -15
   int CIRCLE_TOUCH = 20
   int LINE_TOUCH = 10   
   
   List<Point> polygon
   Point drag
   Paint pCirc
   Paint pCircDrag
   Paint pLine
   Paint pLineCurrent
   Paint pLineSelected
   Point touchStart
   
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

      pCircDrag = new Paint()
      pCircDrag.setARGB(255, 0, 0, 255)


      pLine = new Paint()
      pLine.setARGB(255, 0, 255, 0)

      pLineCurrent = new Paint()
      pLineCurrent.setARGB(255, 0, 255, 255)

      pLineSelected = new Paint()
      pLineSelected.setARGB(255, 255, 255, 0)

      
      isSPen = SDrawLibrary::supportedModel
      if (isSPen) {
         circRadius = CIRCLE_SPEN
         pLine.setStrokeWidth(LINE_SPEN)
         pLineCurrent.setStrokeWidth(LINE_SPEN)
         pLineSelected.setStrokeWidth(LINE_SPEN)
      } else {
         circRadius = CIRCLE_TOUCH
         pLine.setStrokeWidth(LINE_TOUCH)
         pLineCurrent.setStrokeWidth(LINE_TOUCH)
         pLineSelected.setStrokeWidth(LINE_TOUCH)
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
      canvas.drawLine(pp.x, pp.y, p.x, p.y, pLineCurrent)      

      // draw the circles afterwards to place over the line
      for (Point p2 : polygon) {
         var paint = pCirc
         if (p2 == drag) paint = pCircDrag
         canvas.drawCircle(p2.x, p2.y, circRadius, paint)
      }
         
      super.onDraw(canvas)
   }
   
   override onTouchEvent(MotionEvent event) {
      var handled = false;
      if (event.action == MotionEvent::ACTION_DOWN) {
         drag = null;  

         // find the nearest point to drag
         var Point candidate = getClosestPoint(event.x, event.y, 50)            
         if (candidate != null) {
            drag = candidate
         }         
                
         touchStart = new Point(event.x, event.y)
         handled = true
      }
      
      if (event.action == MotionEvent::ACTION_MOVE) {
         var d = distance(event.x, event.y, touchStart.x, touchStart.y)
         if (drag != null && d > 10) {
            drag.x = event.x
            drag.y = event.y
            invalidate              
            handled = true             
         }      
      }
      
      if (event.action == MotionEvent::ACTION_UP) {
         var x = event.x // new BigDecimal(event.x).add(new BigDecimal(X_CORRECT)).floatValue
         var y = event.y //new BigDecimal(event.y).add(new BigDecimal(Y_CORRECT)).floatValue
      
         if (drag != null && drag.x == event.x && drag.y == event.y) {
               Log::d("vectorpaint",  "drag complete")
            drag.x = x
            drag.y = y
            drag = null // the point is up to date
         } else {
            // add a point here
            drag = null
            polygon.add(new Point(x, y))
         }
         
         touchStart = null
         invalidate              
         handled = true             
      }
      
    handled || super.onTouchEvent(event)
   }
   
   /**
    * Find closest point in polygon
    */
   def getClosestPoint(double x, double y, double radius) {
      var Point ret = null;
      
      var double dist = Double::MAX_VALUE;
      for (Point p : polygon) {
         var d = distance(x, y, p.x, p.y)
         if (d <= radius && dist > d) {
            dist = d
            ret = p
         }
      }      
      
      ret
   }
 
   /**
    * Distance between points
    */
   def double distance(double x1, double y1, double x2, double y2) {
      var x = Math::pow(x2 - x1, 2)
      var y = Math::pow(y2 - y1, 2)
      
      Math::sqrt(x+y)
   }
 
   // clear the canvas and start a new drawing
   def clear() {
      polygon.clear
      drag = null
      invalidate
   }  
   
   // save the shape and preview in 3D using STL viewer
   def preview() {
      var sdDir = Environment::externalStorageDirectory.absolutePath
      if (ExtrudePoly::saveToSTL(polygon, sdDir + "/paint3d.stl")) {
         var f = new File(sdDir + "/paint3d.stl");
         var i = new Intent();
         i.setAction(Intent::ACTION_VIEW);
         i.setDataAndType(Uri::fromFile(f), "");
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
   
   /**
    * Save the polygon data into file. Format: 
    * Each line is a polygon, with first line being the boundary polygon. Line format:
    *  x1:y1,x2:y2,...,
    * Note: line can terminate with a delimiter
    */
   def saveDrawing(String path) {
      var f = new File(path)
      var dir = new File(f.absolutePath)
      dir.mkdirs
      var fBak = new File(path + "~")
      if (f.exists) {
         if (fBak.exists) fBak.delete
         f.renameTo(fBak)
      }
      
      var bs = new FileOutputStream(f)
      for (Point p : polygon) {
         var s = String::valueOf(p.x) + ":" + String::valueOf(p.y) + ","
         bs.write(s.bytes)
      }
      bs.close();
   }

   /**
    * Load polygon data from file. See saveDrawing() javadoc for format
    */
   def loadDrawing(String path) {
      var f = new File(path)
      if (!f.exists) return false
      
      clear
      var is = new FileInputStream(f)
      var int c
      while(c != -1) {
         var s = new StringBuffer()
         c = is.read
         while (c != '\n' && c != -1) {
            s.append(c as char)
         }
         Log::d(Main::LOG_TAG, "")
         
         if (s.length > 0) {
            var points = s.toString().split(",")
            for (String xy : points) {
               if (xy.length > 0 && xy.indexOf(":") > 0) {
                  var axy = xy.split(":")
                  var x = Integer::parseInt(axy.get(0))
                  var y = Integer::parseInt(axy.get(1))
                  polygon.add(new Point(x, y))
               }
            }          
         }
      }
      
      invalidate
      true
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
