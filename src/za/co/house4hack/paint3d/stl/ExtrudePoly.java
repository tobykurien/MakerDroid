package za.co.house4hack.paint3d.stl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;

import za.co.house4hack.paint3d.Main;
import android.util.Log;

public class ExtrudePoly {
   private static final int POLYTYPE_ENCLOSURE = 0;
   private static final int POLYTYPE_HOLE = 1;
   private static final int POLYTYPE_EXTS = 2;

   public ExtrudePoly() {
      // TODO Auto-generated constructor stub
   }

   /**
    * Takes a CounterClockWise list of vertices and extrudes to a given height
    * 
    * @param pointlist
    *           - counter clockwise list of vertices
    * @param height
    *           - height of extrusion
    * @return - TriMesh object with list of triangles of type DTriangle
    * @throws DelaunayError
    */
   public TriMesh polyToTriMesh(Vertex[] pointlist, float height) throws DelaunayError {
      TriMesh ptt = new TriMesh();
      ptt.add(pointlist, new Vertex(0, 0, -1));

      Vertex[] top = new Vertex[pointlist.length];
      for (int i = 0; i < pointlist.length; i++) {
         top[i] = new Vertex(pointlist[i].x, pointlist[i].y, pointlist[i].z + height);
      }
      ptt.add(top, new Vertex(0, 0, 1));

      for (int i = 0; i < pointlist.length - 1; i++) {
         Vertex[] side = new Vertex[5];
         side[0] = pointlist[i];
         side[1] = pointlist[i + 1];
         side[2] = top[i + 1];
         side[3] = top[i];
         side[4] = pointlist[i];
         ptt.add(side);
      }

      return ptt;
   }

   /**
    * Extrudes a polygon, with holes and extra extrusion
    * 
    * @param enclosure
    *           - base shape for the extrusion, specified as a counterclockwise
    *           array of vertices
    * @param holes
    *           - a list of vertex arrays, each specifiying a hole in the base -
    *           counterclockwise
    * @param exts
    *           - list of vertex arrays, each specifiying an extra extrusion on
    *           the base - counterclockwise. If nested, must be specified with
    *           smaller polygon AFTER larger polygon.
    * @param height
    *           - the height of the base extrusion
    * @param fromheight
    *           - the absolute starting height of the extra extrusions - must be
    *           the same length as the exts
    * @param toHeight
    *           - the absolute height of the extra extrusions - must be the same
    *           length as the exts
    * @return
    * @throws DelaunayError
    */
   public TriMesh polyToTriMesh(Vertex[] enclosure, List<Vertex[]> holes, List<Vertex[]> exts, float height, float[] fromHeight,
            float[] toHeight) throws DelaunayError {

      TriMesh ptt = new TriMesh();
      List<Vertex[]> polyList = new ArrayList<Vertex[]>();
      polyList.add(enclosure);
      polyList.addAll(holes);
      polyList.addAll(exts);

      int[] polyTypeList = new int[polyList.size()];
      polyTypeList[0] = POLYTYPE_ENCLOSURE;
      for (int i = 1; i < holes.size() + 1; i++)
         polyTypeList[i] = POLYTYPE_HOLE;
      for (int i = 1 + holes.size(); i < exts.size() + holes.size() + 1; i++)
         polyTypeList[i] = POLYTYPE_EXTS;

      // Add the bottom + top
      List<List<DTriangle>> triList = ptt.build(polyList, new Vertex(0, 0, -1));
      for (int i = 0; i < triList.size(); i++) {
         switch (polyTypeList[i]) {
            case POLYTYPE_ENCLOSURE:
               ptt.add(triList.get(i));
               for (DTriangle d : triList.get(i)) {
                  ptt.add(flipAndTranslate(d, height));
               }
               addSides(polyList.get(i), ptt, 0, height);
               break;
            case POLYTYPE_HOLE:
               addSides(polyList.get(i), ptt, height, 0);
               break;
            case POLYTYPE_EXTS:
               ptt.add(triList.get(i));
               for (DTriangle d : triList.get(i)) {
                  ptt.add(flipAndTranslate(d, toHeight[i - (1 + holes.size())]));
               }
               addSides(polyList.get(i), ptt, fromHeight[i - (1 + holes.size())], toHeight[i - (1 + holes.size())]);
               break;
         }
      }
      return ptt;
   }

   /**
    * Extrudes a polygon, with holes and extra extrusion
    * 
    * @param polyData
    *           - describes the enclosure, holes and exts
    * @param height
    *           - the height of the base extrusion
    * @param extheight
    *           - the height of the extra extrusions
    * @return
    * @throws DelaunayError
    */
   public TriMesh polyToTriMesh(ExtrPolyData polyData, float height, float[] fromHeight, float[] toHeight) throws DelaunayError {
      return polyToTriMesh(polyData.enclosure, polyData.holes, polyData.exts, height, fromHeight, toHeight);
   }

   /**
    * Adds vertical sides to the polygon specified in pointlist and add to the
    * Trimesh ptt
    * 
    * @param pointlist
    *           - polygon as counter-clockwise list of points (closing)
    * @param ptt
    *           - TriMesh - maintains list of triangles and methods to convert
    *           points to triangles
    * @param height1
    *           - see height2
    * @param height2
    *           - if height1 is below height2 the triangle normal will point out
    *           otherwise in
    * @throws DelaunayError
    */
   private void addSides(Vertex[] pointlist, TriMesh ptt, float height1, float height2) throws DelaunayError {
      // add the sides
      for (int i1 = 0; i1 < pointlist.length - 1; i1++) {
         Vertex[] side = new Vertex[5];
         side[0] = new Vertex(pointlist[i1].x, pointlist[i1].y, pointlist[i1].z + height1);
         side[1] = new Vertex(pointlist[i1 + 1].x, pointlist[i1 + 1].y, pointlist[i1 + 1].z + height1);
         side[2] = new Vertex(pointlist[i1 + 1].x, pointlist[i1 + 1].y, pointlist[i1 + 1].z + height2);
         side[3] = new Vertex(pointlist[i1].x, pointlist[i1].y, pointlist[i1].z + height2);
         side[4] = new Vertex(pointlist[i1].x, pointlist[i1].y, pointlist[i1].z + height1);
         ptt.add(side);
      }

   }

   /**
    * Flips a triangle normal and translate to the z-axis with height
    * 
    * @param d
    * @param height
    * @return the translated triangle
    * @throws DelaunayError
    */

   private DTriangle flipAndTranslate(DTriangle d, float height) throws DelaunayError {

      DPoint p0 = new DPoint(d.getPoint(0).getX(), d.getPoint(0).getY(), d.getPoint(0).getZ() + height);
      DPoint p1 = new DPoint(d.getPoint(1).getX(), d.getPoint(1).getY(), d.getPoint(1).getZ() + height);
      DPoint p2 = new DPoint(d.getPoint(2).getX(), d.getPoint(2).getY(), d.getPoint(2).getZ() + height);

      return new DTriangle(p0, p2, p1);
   }

   /**
    * Extrude the polygon and save to an STL file
    * 
    * @param polygon
    * @throws DelaunayError 
    * @throws IOException 
    */
   public static boolean saveToSTL(Layer enclosure, Layer rawRaised, Layer rawSunken, String filePath, int scaleFactor) throws DelaunayError, IOException {
      Layer rawExt = new Layer();
      if (rawRaised != null) rawExt.addAll(rawRaised);
      if (rawSunken != null) rawExt.addAll(rawSunken);
      ExtrPolyData ep = new ExtrPolyData(enclosure, rawExt);

      ep.normalize(scaleFactor);

      File f = new File(filePath);
      f.mkdirs();
      if (f.exists()) f.delete();

      FileWriter outFile = new FileWriter(filePath);
      PrintWriter out = new PrintWriter(outFile);

      ExtrudePoly pto3d = new ExtrudePoly();

      int polys = rawExt.size();
      int raised = (rawRaised != null ? rawRaised.size() : 0);
      float[] fromHeight = new float[polys];
      for (int i = 0; i < fromHeight.length; i++)
         fromHeight[i] = (i < raised ? 5f : 5f);         

      float[] toHeight = new float[polys];
      for (int i = 0; i < toHeight.length; i++)
         toHeight[i] = (i < raised) ? 7f : 2f;

      TriMesh ptt = pto3d.polyToTriMesh(ep, 5f, fromHeight, toHeight);

      out.write(ptt.toSTL());
      out.close();

      return true;
   }

   // public static boolean saveToSTL(List<Point> rawPoly, String filePath) {
   // return saveToSTL(rawPoly,new ArrayList<List<Point>>(),new
   // ArrayList<List<Point>>(),new ArrayList<List<Point>>(),filePath );
   // }
   //

   public static void main(String[] args) throws DelaunayError {
      try {
         FileWriter outFile = new FileWriter("/home/schalk/tmp/test1.stl");
         PrintWriter out = new PrintWriter(outFile);

         ExtrudePoly pto3d = new ExtrudePoly();

         // outside
         ArrayList<Vertex> pointlist = new ArrayList<Vertex>();
         pointlist.add(new Vertex(0.0f, 0.0f, 0.0f));
         pointlist.add(new Vertex(10.0f, 0.0f, 0.0f));
         pointlist.add(new Vertex(10.0f, 10.0f, 0.0f));
         pointlist.add(new Vertex(7.5f, 5.0f, 0.0f));
         pointlist.add(new Vertex(5.0f, 10.0f, 0.0f));
         pointlist.add(new Vertex(2.5f, 5.0f, 0.0f));
         pointlist.add(new Vertex(1.25f, 10.0f, 0.0f));
         pointlist.add(new Vertex(0.0f, 5.0f, 0.0f));
         pointlist.add(new Vertex(0.0f, 0.0f, 0.0f));

         // hole
         ArrayList<Vertex[]> holeList = new ArrayList<Vertex[]>();
/*
         ArrayList<Vertex> hole1 = new ArrayList<Vertex>();
         hole1.add(new Vertex(1.0f, 1.0f, 0.0f));
         hole1.add(new Vertex(2.0f, 1.0f, 0.0f));
         hole1.add(new Vertex(2.0f, 2.0f, 0.0f));
         hole1.add(new Vertex(1.0f, 2.0f, 0.0f));
         hole1.add(new Vertex(1.0f, 1.0f, 0.0f));
         holeList.add(hole1.toArray(new Vertex[0]));

         ArrayList<Vertex> hole2 = new ArrayList<Vertex>();
         hole2.add(new Vertex(4.0f, 4.0f, 0.0f));
         hole2.add(new Vertex(5.0f, 4.0f, 0.0f));
         hole2.add(new Vertex(5.0f, 5.0f, 0.0f));
         hole2.add(new Vertex(4.0f, 5.0f, 0.0f));
         hole2.add(new Vertex(4.0f, 4.0f, 0.0f));
         holeList.add(hole2.toArray(new Vertex[0]));
*/
         // ext
         ArrayList<Vertex[]> extList = new ArrayList<Vertex[]>();
  /*       ArrayList<Vertex> ext1 = new ArrayList<Vertex>();
         ext1.add(new Vertex(3.0f, 3.0f, 0.0f));
         ext1.add(new Vertex(3.5f, 3.0f, 0.0f));
         ext1.add(new Vertex(3.5f, 3.5f, 0.0f));
         ext1.add(new Vertex(3.25f, 3.75f, 0.0f));
         ext1.add(new Vertex(3.0f, 3.5f, 0.0f));
         ext1.add(new Vertex(3.0f, 3.0f, 0.0f));
         extList.add(ext1.toArray(new Vertex[0]));

         // ext
         ArrayList<Vertex> ext3 = new ArrayList<Vertex>();
         ext3.add(new Vertex(6.5f, 1.5f, 0.0f));
         ext3.add(new Vertex(8.0f, 1.5f, 0.0f));
         ext3.add(new Vertex(8.0f, 3.0f, 0.0f));
         ext3.add(new Vertex(6.5f, 3.0f, 0.0f));
         ext3.add(new Vertex(6.5f, 1.5f, 0.0f));
         extList.add(ext3.toArray(new Vertex[0]));

         // ext
         ArrayList<Vertex> ext2 = new ArrayList<Vertex>();
         ext2.add(new Vertex(7.0f, 2.0f, 0.0f));
         ext2.add(new Vertex(7.5f, 2.0f, 0.0f));
         ext2.add(new Vertex(7.5f, 2.5f, 0.0f));
         ext2.add(new Vertex(7.0f, 2.5f, 0.0f));
         ext2.add(new Vertex(7.0f, 2.0f, 0.0f));
         extList.add(ext2.toArray(new Vertex[0]));
*/
         ExtrPolyData ep = new ExtrPolyData(pointlist.toArray(new Vertex[0]), holeList, extList);
         ep.normalize(50);

         TriMesh ptt = pto3d.polyToTriMesh(ep, 10f, new float[] { 10f, 10f, 7.5f }, new float[] { 12.5f, 7.5f, 12.5f });

         out.write(ptt.toSTL());
         out.close();

      } catch (IOException e) {
         e.printStackTrace();
      }
   }

}
