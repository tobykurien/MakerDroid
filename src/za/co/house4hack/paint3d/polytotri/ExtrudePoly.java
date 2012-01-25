package za.co.house4hack.paint3d.polytotri;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jdelaunay.delaunay.error.DelaunayError;

import za.co.house4hack.paint3d.Main;
import za.co.house4hack.paint3d.stl.Point;
import za.co.house4hack.paint3d.stl.Vertex;
import android.util.Log;

public class ExtrudePoly {
   public ExtrudePoly() {
	// TODO Auto-generated constructor stub
}
   /**
    * Takes a CounterClockWise list of vertices and extrudes to a given height  
    * @param pointlist - counter clockwise list of vertices
    * @param height - height of extrusion
    * @return - TriMesh object with list of triangles of type DTriangle
    * @throws DelaunayError
    */
   public TriMesh polyToTriMesh(Vertex[] pointlist, float height) throws DelaunayError{
	   TriMesh ptt = new TriMesh();
	   ptt.add(pointlist,new Vertex(0,0,-1));
	   
	   Vertex[] top = new Vertex[pointlist.length];
	   for(int i=0; i< pointlist.length; i++)  {
		       top[i] = new Vertex(pointlist[i].x, pointlist[i].y, pointlist[i].z+height);
	   }
	   ptt.add(top,new Vertex(0,0,1));
	   
	   for(int i=0; i<pointlist.length-1; i++){
		   Vertex[] side = new Vertex[5];
		   side[0] = pointlist[i];
		   side[1] = pointlist[i+1];
		   side[2] = top[i+1];
		   side[3] = top[i];
		   side[4] = pointlist[i];
		   ptt.add(side);
	   }
	   
	   return ptt;
   }
   
   public TriMesh polyToTriMesh(Point[] pointlist, float height, float z) throws DelaunayError{
	   Vertex[] vlist  = new Vertex[pointlist.length];
	   for(int i=0; i<pointlist.length; i++){
		   vlist[i] = new Vertex(pointlist[i].x, pointlist[i].y,z);
	   }
	   return polyToTriMesh(vlist, height);
   }

   public TriMesh polyToTriMesh(List<Point> pointlist, float height, float z) throws DelaunayError{
	   Vertex[] vlist  = new Vertex[pointlist.size()];
	   for(int i=0; i<pointlist.size(); i++){
		   vlist[i] = new Vertex(pointlist.get(i).x, pointlist.get(i).y,z);
	   }
	   return polyToTriMesh(vlist, height);
   }
   
   /**
    * Extrude the polygon and save to an STL file
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
      } catch (IOException e){
         Log.e(Main.LOG_TAG, "error saving STL", e);
      } catch (DelaunayError e) {
         Log.e(Main.LOG_TAG, "error saving STL", e);
      } 
      
      return false;
   }
   
   /**
    * Translate and scale the object for printing. 
    * @param rawPoly
    * @param max - the maximum size (i.e. max width or height, whichever is greater)
    * @return
    */
   private static List<Point> normalize(List<Point> rawPoly, float max) {
      List<Point> ret = new ArrayList<Point>();
      
      float maxX = 0;
      float maxY = 0;
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      for (Point p: rawPoly) {
         if (minX > p.x) minX = p.x;
         if (minY > p.y) minY = p.y;
         if (maxX < p.x) maxX = p.x;
         if (maxY < p.y) maxY = p.y;
      }
      
      float scaleMax = (maxX > maxY ? maxX : maxY);
      float scale = max/scaleMax;
      for (Point p: rawPoly) {
         Point np = new Point((p.x - minX)*scale, (p.y - minY)*scale);
         ret.add(np);
      }
      
      return ret;
   }
   public static void main(String[] args) throws DelaunayError {
	   
	 		try {
	 			FileWriter outFile = new FileWriter("/home/schalk/tmp/test.stl");
	 			PrintWriter out = new PrintWriter(outFile);
	 			ArrayList<Vertex> pointlist = new ArrayList<Vertex>();
	 			
	 			ExtrudePoly pto3d = new ExtrudePoly();

	 			pointlist.add(new Vertex(0.0f,   0.0f, 0.0f));
	 			pointlist.add(new Vertex(10.0f,  0.0f, 0.0f));
	 			pointlist.add(new Vertex(10.0f,  10.0f, 0.0f));
	 			pointlist.add(new Vertex(7.5f,   5.0f, 0.0f));
	 			pointlist.add(new Vertex(5.0f,   10.0f, 0.0f));
	 			pointlist.add(new Vertex(2.5f,   5.0f, 0.0f));
	 			pointlist.add(new Vertex(1.25f,  10.0f, 0.0f));
	 			pointlist.add(new Vertex(0.0f,   5.0f, 0.0f));
	 			pointlist.add(new Vertex(0.0f,   0.0f, 0.0f));
	 			
	 			
	 			TriMesh ptt = pto3d.polyToTriMesh(pointlist.toArray(new Vertex[0]), 1f);

	 			out.write(ptt.toSTL());	
	 			out.close();

	 		} catch (IOException e){
	 			e.printStackTrace();
	 		} 
}
}
