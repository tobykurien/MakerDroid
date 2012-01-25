package za.co.house4hack.paint3d;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import za.co.house4hack.paint3d.stl.ExtrudePoly;
import za.co.house4hack.paint3d.stl.Point;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.samsung.sdraw.SDrawLibrary;

class VectorPaint extends View {
   int CIRCLE_SPEN = 5;
   int LINE_SPEN = 2;
   int CIRCLE_TOUCH = 20;
   int LINE_TOUCH = 10;

   List<Point> polygon;
   Point drag;
   Paint pCirc;
   Paint pCircDrag;
   Paint pLine;
   Paint pLineCurrent;
   Paint pLineSelected;
   Point touchStart;

   boolean isSPen = false;
   boolean isDragging = false;
   int circRadius;

   public VectorPaint(Context context) {
      super(context);
      init();
   }

   public VectorPaint(Context arg0, AttributeSet arg1) {
      super(arg0, arg1);
      init();
   }

   public VectorPaint(Context arg0, AttributeSet arg1, int arg2) {
      super(arg0, arg1, arg2);
      init();
   }

   public void init() {
      polygon = new ArrayList<Point>();

      pCirc = new Paint();
      pCirc.setARGB(255, 255, 0, 0);

      pCircDrag = new Paint();
      pCircDrag.setARGB(255, 0, 0, 255);

      pLine = new Paint();
      pLine.setARGB(255, 0, 255, 0);

      pLineCurrent = new Paint();
      pLineCurrent.setARGB(255, 0, 255, 255);

      pLineSelected = new Paint();
      pLineSelected.setARGB(255, 255, 255, 0);

      isSPen = SDrawLibrary.isSupportedModel();
      if (isSPen) {
         circRadius = CIRCLE_SPEN;
         pLine.setStrokeWidth(LINE_SPEN);
         pLineCurrent.setStrokeWidth(LINE_SPEN);
         pLineSelected.setStrokeWidth(LINE_SPEN);
      } else {
         circRadius = CIRCLE_TOUCH;
         pLine.setStrokeWidth(LINE_TOUCH);
         pLineCurrent.setStrokeWidth(LINE_TOUCH);
         pLineSelected.setStrokeWidth(LINE_TOUCH);
      }
   }

   protected void onDraw(Canvas canvas) {
      if (polygon.isEmpty()) {
         Paint pText = new Paint();
         pText.setARGB(255, 255, 255, 255);
         pText.setTextSize(40);
         canvas.drawText(getHelpText(), 20, 40, pText);
         return;
      }

      // draw points where canvas was touched
      Point pp = null;
      for (Point p : polygon) {
         if (pp != null) {
            canvas.drawLine(pp.x, pp.y, p.x, p.y, pLine);
         }
         pp = p;
      }

      // complete the polygon
      Point p = polygon.get(0);
      canvas.drawLine(pp.x, pp.y, p.x, p.y, pLineCurrent);

      // draw the circles afterwards to place over the line
      for (Point p2 : polygon) {
         Paint paint = pCirc;
         if (p2 == drag) paint = pCircDrag;
         canvas.drawCircle(p2.x, p2.y, circRadius, paint);
      }

      super.onDraw(canvas);
   }

   public boolean onTouchEvent(MotionEvent event) {
      boolean handled = false;
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
         drag = null;
         isDragging = false;

         // find nearest line to split
         int idx = -1;
         double dist = Double.MAX_VALUE;
         for (int i=0; i < polygon.size() - 1; i++) {
            Point p1 = polygon.get(i);
            Point p2 = polygon.get(i+1);
            Point p3 = new Point(event.getX(), event.getY());
            
            // add threshold from line endpoints
            if (distance(p1, p3) > 40 && distance(p2, p3) > 40) {
               double d = distanceToSegment(p1, p2, p3);
               if (d < 50 && d < dist) {
                  dist = d;
                  idx = i;
                  drag = p3;
               }
            }
         }
         
         if (idx >= 0) {
            // split line by adding a point and then drag it
            polygon.add(idx+1, drag);
            isDragging = true;
         }
         
         if (!isDragging) {
            // find the nearest point to drag
            Point candidate = getClosestPoint(event.getX(), event.getY(), 20);
            if (candidate != null) {
               drag = candidate;
            }
         }

         touchStart = new Point(event.getX(), event.getY());
         handled = true;
      }

      if (event.getAction() == MotionEvent.ACTION_MOVE) {
         if (!isDragging && drag != null) {
            // add a threshold over which dragging is initiated
            double d = distance(new Point(event.getX(), event.getY()), touchStart);
            if (d > 10) {
               isDragging = true;
            }
         }

         if (isDragging && drag != null) {
            drag.x = event.getX();
            drag.y = event.getY();
            invalidate();
            handled = true;
         }
      }

      if (event.getAction() == MotionEvent.ACTION_UP) {
         float x = event.getX();
         float y = event.getY();

         if (drag != null && isDragging) {
            drag.x = x;
            drag.y = y;
         } else {
            // add a point here
            polygon.add(new Point(x, y));
         }

         drag = null;
         touchStart = null;
         isDragging = false;
         invalidate();
         handled = true;
      }

      return handled || super.onTouchEvent(event);
   }

   /**
    * Find closest point in polygon
    */
   public Point getClosestPoint(double x, double y, double radius) {
      Point ret = null;

      double dist = Double.MAX_VALUE;
      for (Point p : polygon) {
         double d = distance(new Point(x,y), p);
         if (d <= radius && dist > d) {
            dist = d;
            ret = p;
            break;
         }
      }

      return ret;
   }

   /**
    * Distance between points
    */
   public static double distance(Point p1, Point p2) {
      double x = Math.pow(p2.x - p1.x, 2);
      double y = Math.pow(p2.y - p1.y, 2);
      return Math.sqrt(x + y);
   }

   /**
    * Returns the distance of p3 to the segment defined by p1,p2;
    * 
    * @param p1
    *           First point of the segment
    * @param p2
    *           Second point of the segment
    * @param p3
    *           Point to which we want to know the distance of the segment
    *           defined by p1,p2
    * @return The distance of p3 to the segment defined by p1,p2
    */
   public static double distanceToSegment(Point p1, Point p2, Point p3) {

      final double xDelta = p2.x - p1.x;
      final double yDelta = p2.y - p1.y;

      if ((xDelta == 0) && (yDelta == 0)) { throw new IllegalArgumentException("p1 and p2 cannot be the same point"); }

      final double u = ((p3.x - p1.x) * xDelta + (p3.y - p1.y) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

      final Point closestPoint;
      if (u < 0) {
         closestPoint = p1;
      } else if (u > 1) {
         closestPoint = p2;
      } else {
         closestPoint = new Point(p1.x + u * xDelta, p1.y + u * yDelta);
      }
      
      return distance(p3, closestPoint);
   }

   // clear the canvas and start a new drawing
   public void clear() {
      polygon.clear();
      drag = null;
      invalidate();
   }

   // save the shape and preview in 3D using STL viewer
   public void preview() {
      String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath();
      if (ExtrudePoly.saveToSTL(polygon, sdDir + "/paint3d.stl")) {
         File f = new File(sdDir + "/paint3d.stl");
         Intent i = new Intent();
         i.setAction(Intent.ACTION_VIEW);
         i.setDataAndType(Uri.fromFile(f), "");
         getContext().startActivity(i);
      }
   }

   // undo last point
   public void undo() {
      polygon.remove(polygon.size() - 1);
      invalidate();
   }

   // change brush colour and create the etching polygon
   public void etch() {

   }

   /**
    * Save the polygon data into file. Format:
    * Each line is a polygon, with first line being the boundary polygon. Line
    * format:
    * x1:y1,x2:y2,...,
    * Note: line can terminate with a delimiter
    */
   public void saveDrawing(String path) {
      File f = new File(path);
      File dir = new File(f.getAbsolutePath());
      dir.mkdirs();

      File fBak = new File(path + "~");
      if (f.exists()) {
         if (fBak.exists()) fBak.delete();
         f.renameTo(fBak);
      }

      try {
         FileOutputStream bs = new FileOutputStream(f);
         for (Point p : polygon) {
            String s = String.valueOf(p.x) + ":" + String.valueOf(p.y) + ",";
            bs.write(s.getBytes());
         }
         bs.close();
      } catch (Exception e) {
         Toast.makeText(getContext(), "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
      }
   }

   /**
    * Load polygon data from file. See saveDrawing() javadoc for format
    */
   public boolean loadDrawing(String path) {
      File f = new File(path);
      if (!f.exists()) return false;

      try {
         clear();
         FileInputStream is = new FileInputStream(f);
         int c = 0;
         while (c != -1) {
            c = is.read();
            StringBuffer s = new StringBuffer();
            while (c != '\n' && c != -1) {
               s.append((char) c);
               c = is.read();
            }

            if (s.length() > 0) {
               String[] points = s.toString().split(",");
               for (String xy : points) {
                  if (xy.length() > 0 && xy.indexOf(":") > 0) {
                     String[] axy = xy.split(":");
                     float x = Float.parseFloat(axy[0]);
                     float y = Float.parseFloat(axy[1]);
                     polygon.add(new Point(x, y));
                  }
               }
            }
         }
      } catch (Exception e) {
         Toast.makeText(getContext(), "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
      }

      invalidate();
      return true;
   }

   public String getHelpText() {
      if (isSPen) {
         return getString(R.string.vp_help_spen);
      } else {
         return getString(R.string.vp_help_touch);
      }
   }

   public String getString(int resId) {
      return getContext().getResources().getString(resId);
   }
}