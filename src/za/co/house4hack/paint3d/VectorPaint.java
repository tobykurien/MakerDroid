package za.co.house4hack.paint3d;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.jdelaunay.delaunay.error.DelaunayError;

import za.co.house4hack.paint3d.gcode.SkeinforgeWrapper;
import za.co.house4hack.paint3d.stl.ExtrudePoly;
import za.co.house4hack.paint3d.stl.Layer;
import za.co.house4hack.paint3d.stl.Point;
import za.co.house4hack.paint3d.stl.Polygon;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.samsung.sdraw.SDrawLibrary;

class VectorPaint extends View {
   // line and circle sizes
   int CIRCLE_SPEN = 5;
   int LINE_SPEN = 2;
   int CIRCLE_TOUCH = 20;
   int LINE_TOUCH = 10;

   // thresholds
   int LINE_BREAK = 30; // pixel distance to line to activate line-break
   int POINT_DRAG = 50; // pixel distance to point to activate it for dragging
   int DRAG_ACTIVATE = 5; // pixels to drag before dragging is activated

   // Other magic numbers
   int SCALE_MAX = 100; // scale the object to be around 6cm

   // data storage
   Polygon polygon;
   List<Layer> layers; // each layer has many polygons with many points
   Point drag;
   Paint pCirc;
   Paint pCircDrag;
   Paint pLine;
   Paint pLineCurrent;
   Paint pLineSelected;
   Point touchStart;
   int layer = 0;
   int poly = 0;

   Stack<Undo> undoHistory;

   // state flags
   boolean isSPen = false;
   boolean isDragging = false;
   int circRadius;

   /**
    * Class to store undo operation
    * 
    * @author tobykurien
    * 
    */
   public class Undo {
      public boolean isMove = false;
      public Point lastPoint;
      public Point beforeMove;

      public Undo(boolean isMove, Point point, Point touchStart) {
         this.isMove = isMove;
         this.lastPoint = point;
         this.beforeMove = touchStart;
      }
   }

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
      layer = 0;
      poly = 0;
      layers = new ArrayList();
      layers.add(new Layer());
      polygon = layers.get(layer).get(poly);

      pCirc = new Paint();
      pCirc.setARGB(255, 255, 0, 0);

      pCircDrag = new Paint();
      pCircDrag.setARGB(255, 0, 0, 255);

      pLine = new Paint();
      pLine.setARGB(255, 0, 255, 0);
      // pLine.setMaskFilter(new EmbossMaskFilter(new float[] { 1, 1, 1 }, 0.4f,
      // 6, 3.5f));

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

      undoHistory = new Stack<Undo>();

      loadPrefs();
   }

   public void loadPrefs() {
      SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
      try {
         LINE_BREAK = Integer.parseInt(pref.getString("line_dist", "" + LINE_BREAK));
      } catch (Exception e) {
      }
      try {
         POINT_DRAG = Integer.parseInt(pref.getString("drag_radius", "" + POINT_DRAG));
      } catch (Exception e) {
      }
      try {
         SCALE_MAX = Integer.parseInt(pref.getString("print_size", "" + SCALE_MAX));
      } catch (Exception e) {
      }
   }

   protected void onDraw(Canvas canvas) {
      if (layers.isEmpty() || layers.get(0).isEmpty() || layers.get(0).get(0).isEmpty()) {
         Paint pText = new Paint();
         pText.setARGB(255, 255, 255, 255);
         pText.setTextSize(40);
         canvas.drawText(getHelpText(), 20, 40, pText);
         return;
      }

      for (Layer l : layers) {
         for (Polygon pol : l) {
            if (!pol.isEmpty()) drawPoly(canvas, pol, polygon == pol, layers.indexOf(l));
         }
      }
      super.onDraw(canvas);
   }

   private void drawPoly(Canvas canvas, List<Point> polygon, boolean isCurrent, int layer) {
      // draw points where canvas was touched
      Point pp = null;
      for (Point p : polygon) {
         if (pp != null) {
            if (layer == 0) pLine.setARGB(255, 0, 255, 0);
            if (layer == 1) pLine.setARGB(255, 255, 155, 155);
            canvas.drawLine(pp.x, pp.y, p.x, p.y, pLine);
         }
         pp = p;
      }

      // complete the polygon
      Point p = polygon.get(0);
      canvas.drawLine(pp.x, pp.y, p.x, p.y, (isCurrent ? pLineCurrent : pLine));

      // draw the circles afterwards to place over the line
      if (isCurrent) {
         for (Point p2 : polygon) {
            Paint paint = pCirc;
            if (p2 == drag) paint = pCircDrag;
            canvas.drawCircle(p2.x, p2.y, circRadius, paint);
         }
      }
   }

   public boolean onTouchEvent(MotionEvent event) {
      boolean handled = false;
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
         touchStart = new Point(event.getX(), event.getY());
         drag = null;
         isDragging = false;

         // find nearest line to split
         int idx = -1;
         double dist = Double.MAX_VALUE;
         for (int i = 0; i < polygon.size() - 1; i++) {
            Point p1 = polygon.get(i);
            Point p2 = polygon.get(i + 1);
            Point p3 = new Point(event.getX(), event.getY());

            // add threshold from line endpoints
            double d = distanceToSegment(p1, p2, p3, 20);
            if (d < LINE_BREAK && d < dist) {
               dist = d;
               idx = i;
               drag = p3;
            }
         }

         if (idx >= 0) {
            // split line by adding a point and then drag it
            polygon.add(idx + 1, drag);
            isDragging = true;
            undoHistory.push(new Undo(false, drag, null));
         }

         if (!isDragging) {
            // find the nearest point to drag
            Point candidate = getClosestPoint(event.getX(), event.getY(), POINT_DRAG);
            if (candidate != null) {
               drag = candidate;
            }
         }

         handled = true;
      }

      if (event.getAction() == MotionEvent.ACTION_MOVE) {
         if (!isDragging && drag != null) {
            // add a threshold over which dragging is initiated
            double d = distance(new Point(event.getX(), event.getY()), touchStart);
            if (d > DRAG_ACTIVATE) {
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
            undoHistory.push(new Undo(true, drag, touchStart));
         } else {
            // add a point here
            Point p = new Point(x, y);
            polygon.add(p);
            undoHistory.push(new Undo(false, p, null));
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
         double d = distance(new Point(x, y), p);
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
   public static double distanceToSegment(Point p1, Point p2, Point p3, double threshold) {

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

      if (distance(closestPoint, p1) < threshold || distance(closestPoint, p2) < threshold) {
         // disregard this as it's too close to the line endpoints
         return Double.MAX_VALUE;
      }

      return distance(p3, closestPoint);
   }

   // clear the canvas and start a new drawing
   public void clear() {
      init();
      undoHistory.clear();
      drag = null;
      invalidate();
   }

   public boolean drawingEmpty() {
      try {
         return layers.get(0).get(0).size() == 0;
      } catch (Exception e) {
         return true;
      }
   }

   // save the shape and preview in 3D using STL viewer
   public void preview(String fullFilePath) throws DelaunayError, IOException {
      String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath();

      ExtrudePoly.saveToSTL(layers.get(0), null, (layers.size() > 1 ? layers.get(1) : null), fullFilePath, SCALE_MAX);
      File f = new File(fullFilePath);
      Intent i = new Intent();
      i.setAction(Intent.ACTION_VIEW);
      i.setDataAndType(Uri.fromFile(f), "");
      getContext().startActivity(i);
   }

   public void print(String fullFilePath, String printerModel) throws Exception {
      if (drawingEmpty()) {
         throw new Exception(getString(R.string.err_drawing_empty));
      }

      ExtrudePoly.saveToSTL(layers.get(0), null, (layers.size() > 1 ? layers.get(1) : null), fullFilePath, SCALE_MAX);
      SkeinforgeWrapper sw = new SkeinforgeWrapper(this.getContext());
      sw.generateGcode(fullFilePath, fullFilePath + ".log", printerModel);
   }

   // undo last point
   public void undo() {
      if (undoHistory.isEmpty()) return;

      Undo undo = undoHistory.pop();
      if (undo.isMove) {
         undo.lastPoint.x = undo.beforeMove.x;
         undo.lastPoint.y = undo.beforeMove.y;
      } else {
         polygon.remove(undo.lastPoint);
      }

      invalidate();
   }

   /**
    * Switch to another polygon in the layer or add a new polygon
    */
   public void newPoly() {
      Layer curLayer = layers.get(layer);
      poly++;
      if (poly >= layers.get(layer).size()) {
         // at this point we either go back to first polygon in the layer or add
         // a new one
         if (poly > 0 && layers.get(layer).get(poly - 1).size() == 0) {
            // current polygon is already blank so don't add another one, switch
            // back to first poly
            poly = 0;
         } else {
            // add a new polygon to this layer
            curLayer.add(new Polygon());
            poly = curLayer.size() - 1;
         }
      }
      polygon = curLayer.get(poly);
      undoHistory.clear();
      invalidate();
   }

   // switch between 2 layers
   public void layer() {
      if (layer == 0 && !layers.get(0).get(0).isEmpty()) {
         layer = 1;
         poly = 0;
         if (layers.size() == 1) {
            layers.add(new Layer());
         }
      } else {
         layer = 0;
         poly = 0;
      }
      polygon = layers.get(layer).get(poly);
      undoHistory.clear();
      invalidate();
   }

   /**
    * Save the polygon data into file. Format:
    * Each line in the file is a polygon, with first line being the boundary
    * polygon. Line
    * format:
    * x1:y1,x2:y2,...,
    * Note: line can terminate with a delimiter
    * When switching to a new layer, the layer count appears on it's own on a
    * new line,
    * followed by it's polygons as above
    * 
    * @throws IOException
    */
   public void saveDrawing(String path) throws IOException {
      File f = new File(path);
      File dir = new File(f.getAbsolutePath());
      dir.mkdirs();

      File fBak = new File(path + "~");
      if (f.exists()) {
         if (fBak.exists()) fBak.delete();
         f.renameTo(fBak);
      }

      FileOutputStream bs = new FileOutputStream(f);
      int lcount = 0;
      for (Layer l : layers) {
         if (lcount > 0) {
            // write the layer number into the file
            bs.write(String.valueOf(lcount).getBytes());
            bs.write('\n');
         }

         for (Polygon pol : l) {
            for (Point p : pol) {
               String s = String.valueOf(p.x) + ":" + String.valueOf(p.y) + ",";
               bs.write(s.getBytes());
            }
            bs.write('\n');
         }
         lcount++;
      }
      bs.close();
   }

   /**
    * Load polygon data from file. See saveDrawing() javadoc for format
    * 
    * @throws IOException
    */
   public boolean loadDrawing(String path) throws IOException {
      File f = new File(path);
      if (!f.exists()) return false;

      clear();
      layers.get(0).clear();
      FileInputStream is = new FileInputStream(f);
      int c = 0;
      int pcount = 0;
      while (c != -1) {
         c = is.read();
         StringBuffer s = new StringBuffer();
         while (c != '\n' && c != -1) {
            s.append((char) c);
            c = is.read();
         }

         if (s.length() > 0) {
            String[] points = s.toString().split(",");

            // check for a new layer
            if (points.length == 1) {
               // we will ignore the layer number
               layers.add(new Layer());
               layers.get(layers.size() - 1).clear(); // remove the empty
                                                      // polygon
               pcount = 0;
            } else {
               polygon = new Polygon();
               for (String xy : points) {
                  if (xy.length() > 0 && xy.indexOf(":") > 0) {
                     String[] axy = xy.split(":");
                     float x = Float.parseFloat(axy[0]);
                     float y = Float.parseFloat(axy[1]);
                     polygon.add(new Point(x, y));
                  }
               }
               layers.get(layers.size() - 1).add(polygon);
               pcount++;
            }
         }
      }

      layer = 0;
      poly = 0;
      try {
         polygon = layers.get(layer).get(poly);
      } catch (IndexOutOfBoundsException e) {
         clear();
         throw new IOException("Blank or invalid file");
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

   public void deletePoly() {
      if (layers.get(layer).isEmpty()) { return; }
      if (layers.get(layer).get(poly).isEmpty()) { return; }

      layers.get(layer).remove(poly);
      poly--;
      if (poly < 0) poly = 0;
      if (layers.get(layer).isEmpty()) {
         layers.get(layer).add(new Polygon());
      }
      polygon = layers.get(layer).get(poly);
      invalidate();
   }

}
