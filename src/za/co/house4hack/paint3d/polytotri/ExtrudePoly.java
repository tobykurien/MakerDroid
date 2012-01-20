package za.co.house4hack.paint3d.polytotri;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.jdelaunay.delaunay.error.DelaunayError;

import za.co.house4hack.paint3d.Point;
import za.co.house4hack.paint3d.stl.Vertex;

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
