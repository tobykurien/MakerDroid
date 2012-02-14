package za.co.house4hack.paint3d.stl;

import java.util.ArrayList;
import java.util.List;

public class ExtrPolyData {
   Vertex[] enclosure;
   List<Vertex[]> holes;
   List<Vertex[]> exts;

   public ExtrPolyData(Layer enclosure, Layer rawExt) {
      Polygon rawPoly = (Polygon) enclosure.get(0);
      this.enclosure = pointToVertex(rawPoly);

      holes = new ArrayList<Vertex[]>();
      Layer holesLayer = (Layer) enclosure.clone();
      holesLayer.remove(0);
      for (Polygon pl : holesLayer)
         holes.add(pointToVertex(pl));

      exts = new ArrayList<Vertex[]>();
      if (rawExt != null && !rawExt.isEmpty()) {
         for (Polygon pl : rawExt)
            if (pl != null && !pl.isEmpty()) {
               exts.add(pointToVertex(pl));
            }
      }
   }

   public ExtrPolyData(Vertex[] enclosure, ArrayList<Vertex[]> holeList, ArrayList<Vertex[]> extList) {
      this.enclosure = enclosure;
      holes = holeList;
      exts = extList;
   }

   Vertex[] pointToVertex(List<Point> pointlist) {
      Vertex[] result = new Vertex[pointlist.size() + 1];
      for (int i = 0; i < pointlist.size(); i++) {
         Point p = pointlist.get(i);
         result[i] = new Vertex(p.x, p.y, 0.0f);
      }
      Point p = pointlist.get(0);
      result[pointlist.size()] = new Vertex(p.x, p.y, 0.0f);
      return result;
   }

   void normalize(float max) {
      // work out the object's extremities
      float maxX = 0;
      float maxY = 0;
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      for (Vertex p : enclosure) {
         if (minX > p.x) minX = p.x;
         if (minY > p.y) minY = p.y;
         if (maxX < p.x) maxX = p.x;
         if (maxY < p.y) maxY = p.y;
      }

      // resize the object to fit into the given max value
      float scaleMax = (maxX > maxY ? maxX : maxY);
      float scale = max / scaleMax;

      float centreX = minX + (maxX - minX) / 2.0f;
      float centreY = minY + (maxY - minY) / 2.0f;
      enclosure = scalePointList(enclosure, scale, centreX, centreY);
      for (int i = 0; i < holes.size(); i++)
         holes.set(i, scalePointList(holes.get(i), scale, centreX, centreY));
      for (int i = 0; i < exts.size(); i++)
         exts.set(i, scalePointList(exts.get(i), scale, centreX, centreY));

   }

   /**
    * Scale the object and move it to the center point
    * @param pointList
    * @param scale
    * @param centreX
    * @param centreY
    * @return
    */
   Vertex[] scalePointList(Vertex[] pointList, float scale, float centreX, float centreY) {
      Vertex[] result = new Vertex[pointList.length];
      for (int i = 0; i < pointList.length; i++) {
         Vertex p = pointList[i];
         // also flip top to bottom
         result[i] = new Vertex((p.x - centreX) * -1 * scale, (p.y - centreY) * scale, 0.0f); 
      }
      return result;
   }

}
