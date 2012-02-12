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
    * Takes a CounterClockWise list of vertices and extrudes to a given height, caters for holes and extensions
    * 
    * @param pointlist
    *           - counter clockwise list of vertices
    * @param height
    *           - height of extrusion
    * @return - TriMesh object with list of triangles of type DTriangle
    * @throws DelaunayError
    */
   public TriMesh polyToTriMesh(Vertex[] enclosure, List<Vertex[]> holes, List <Vertex[]> exts , float height, float extheight) throws DelaunayError {
      TriMesh ptt = new TriMesh();
      List<Vertex[]> polyList = new ArrayList<Vertex[]>();
      polyList.add(enclosure);
      polyList.addAll(holes);
      polyList.addAll(exts);
      
      int[] polyTypeList = new int[polyList.size()];
      polyTypeList[0] = POLYTYPE_ENCLOSURE;
      for(int i=1; i<holes.size()+1;i++) polyTypeList[i] = POLYTYPE_HOLE;
      for(int i=1+holes.size(); i<exts.size()+holes.size()+1;i++) polyTypeList[i] = POLYTYPE_EXTS;
      
      // Add the bottom + top
      List<List<DTriangle>> triList = ptt.build(polyList, new Vertex(0, 0, -1));
      for(int i=0; i< triList.size();i++){
		switch (polyTypeList[i]){
    	     case POLYTYPE_ENCLOSURE:
    	    	 ptt.add(triList.get(i));
    	    	 for(DTriangle d:triList.get(i)){    	    		 
    	    		 ptt.add(flipAndTranslate(d,height));
    	    	 }    	    	 
                 addSides(polyList.get(i),ptt, 0,height);    	    	 
    	    	 break;
    	     case POLYTYPE_HOLE:
    	    	 addSides(polyList.get(i),ptt, height, 0);
    	    	 break;
    	     case POLYTYPE_EXTS:
    	    	 ptt.add(triList.get(i));
    	    	 for(DTriangle d:triList.get(i)){    	    		 
    	    		 ptt.add(flipAndTranslate(d,height+extheight));
    	    	 }
                 addSides(polyList.get(i),ptt, height, height+extheight);    	    	 
    	    	 break;
    	  }
      }
      return ptt;
   }

   

   private void addSides(Vertex[] pointlist, TriMesh ptt, float height1, float height2) throws DelaunayError {
       // add the sides
       for (int i1 = 0; i1 < pointlist.length - 1; i1++) {
          Vertex[] side = new Vertex[5];
          side[0] = new Vertex(pointlist[i1].x, pointlist[i1].y,pointlist[i1].z+height1);
          side[1] = new Vertex(pointlist[i1 + 1].x, pointlist[i1 + 1].y,pointlist[i1 + 1].z+height1);
          side[2] = new Vertex(pointlist[i1 + 1].x, pointlist[i1 + 1].y,pointlist[i1 + 1].z+height2);
          side[3] = new Vertex(pointlist[i1].x, pointlist[i1].y,pointlist[i1].z+height2);
          side[4] = new Vertex(pointlist[i1].x, pointlist[i1].y,pointlist[i1].z+height1);
          ptt.add(side);
       }

	
}

private DTriangle flipAndTranslate(DTriangle d, float height) throws DelaunayError {
	   
 	 DPoint p0 = new DPoint(d.getPoint(0).getX(),d.getPoint(0).getY(),d.getPoint(0).getZ()+height);
 	 DPoint p1 = new DPoint(d.getPoint(1).getX(),d.getPoint(1).getY(),d.getPoint(1).getZ()+height);
 	 DPoint p2 = new DPoint(d.getPoint(2).getX(),d.getPoint(2).getY(),d.getPoint(2).getZ()+height);
 	 
	return new DTriangle(p0,p2,p1);
}

/**
    * Extrude the polygon and save to an STL file
    * 
    * @param polygon
    */
   public static boolean saveToSTL(List<Point> rawPoly, String filePath) {
      List<Point> polygon = normalize(rawPoly, 50); // 50 = 5cm
      try {
         File f = new File(filePath);
         f.mkdirs();
         if (f.exists()) f.delete();

         FileWriter outFile = new FileWriter(filePath);
         PrintWriter out = new PrintWriter(outFile);
         ArrayList<Vertex> pointlist = new ArrayList<Vertex>();

         ExtrudePoly pto3d = new ExtrudePoly();

         for (Point p : polygon) {
            pointlist.add(new Vertex(p.x, p.y, 0.0f));
         }

         // add first point again to close polygon
         pointlist.add(new Vertex(polygon.get(0).x, polygon.get(0).y, 0.0f));

         TriMesh ptt = pto3d.polyToTriMesh(pointlist.toArray(new Vertex[0]), 5f);

         out.write(ptt.toSTL());
         out.close();

         return true;
      } catch (IOException e) {
         Log.e(Main.LOG_TAG, "error saving STL", e);
      } catch (DelaunayError e) {
         Log.e(Main.LOG_TAG, "error saving STL", e);
      }

      return false;
   }

   /**
    * Translate and scale the object for printing.
    * 
    * @param rawPoly
    * @param max
    *           - the maximum size (i.e. max width or height, whichever is
    *           greater)
    * @return
    */
   private static List<Point> normalize(List<Point> rawPoly, float max) {
      List<Point> ret = new ArrayList<Point>();

      // work out the object's extremities
      float maxX = 0;
      float maxY = 0;
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      for (Point p : rawPoly) {
         if (minX > p.x) minX = p.x;
         if (minY > p.y) minY = p.y;
         if (maxX < p.x) maxX = p.x;
         if (maxY < p.y) maxY = p.y;
      }

      // resize the object to fit into the given max value
      float scaleMax = (maxX > maxY ? maxX : maxY);
      float scale = max / scaleMax;
      
      // center the object around 0,0
      minX *= 2; 
      minY *= 2;
      
      for (Point p : rawPoly) {
         Point np = new Point((p.x - minX) * scale, (p.y - minY) * scale * -1); // also flip top to bottom
         ret.add(np);
      }

      return ret;
   }

   public static void main(String[] args) throws DelaunayError {
      try {
         FileWriter outFile = new FileWriter("/home/schalk/tmp/test.stl");
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
         
         //hole
         ArrayList<Vertex[]> holeList = new ArrayList<Vertex[]>();

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
        

         //ext
         ArrayList<Vertex[]> extList = new ArrayList<Vertex[]>();
         ArrayList<Vertex> ext1 = new ArrayList<Vertex>();
         ext1.add(new Vertex(3.0f, 3.0f, 0.0f));
         ext1.add(new Vertex(3.5f, 3.0f, 0.0f));
         ext1.add(new Vertex(3.5f, 3.5f, 0.0f));
         ext1.add(new Vertex(3.25f, 3.5f, 0.0f));
         ext1.add(new Vertex(3.0f, 3.5f, 0.0f));
         ext1.add(new Vertex(3.0f, 3.0f, 0.0f));
         extList.add(ext1.toArray(new Vertex[0]));

         
         TriMesh ptt = pto3d.polyToTriMesh(pointlist.toArray(new Vertex[0]), holeList, extList, 1f, 0.25f);

         out.write(ptt.toSTL());
         out.close();

      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
