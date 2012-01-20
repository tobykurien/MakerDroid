package za.co.house4hack.paint3d.polytotri;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;

import za.co.house4hack.paint3d.stl.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;


public class TriMesh {
   private GeometryFactory gf;
   private ArrayList<DTriangle> triList = null;

   
   
   public TriMesh() {
	   gf = new GeometryFactory();
	   triList = new ArrayList<DTriangle>();
	   
}
   
   
   /**
    * Convert polygon in pointlist to a list of DTriangle
    * @param pointlist - contains counter clockwise list of vertices
    * @param normal - used for complex polygons to indicate the normal
    * @throws DelaunayError
    */
   
   public void add(Vertex[] pointlist, Vertex normal) throws DelaunayError{
	   if(pointlist.length==5){
		   DPoint p1 = new DPoint(pointlist[0].x,pointlist[0].y, pointlist[0].z);
		   DPoint p2 = new DPoint(pointlist[1].x,pointlist[1].y, pointlist[1].z);
		   DPoint p3 = new DPoint(pointlist[2].x,pointlist[2].y, pointlist[2].z);
		   triList.add(new DTriangle(p1,p2,p3));

		   p1 = new DPoint(pointlist[2].x,pointlist[2].y, pointlist[2].z);
		   p2 = new DPoint(pointlist[3].x,pointlist[3].y, pointlist[3].z);
		   p3 = new DPoint(pointlist[4].x,pointlist[4].y, pointlist[4].z);
		   triList.add(new DTriangle(p1,p2,p3));
		   
	   } else {
		   
		   ConstrainedMesh mesh = new ConstrainedMesh();
		   Coordinate coordinates[] = new Coordinate[pointlist.length];
		   for(int i=0; i<pointlist.length-1; i++){
			   int j = i+1;
				mesh.addConstraintEdge(new DEdge(pointlist[i].x, pointlist[i].y, pointlist[i].z,pointlist[j].x, pointlist[j].y, pointlist[j].z));			
				coordinates[i] = new Coordinate(pointlist[i].x, pointlist[i].y, pointlist[i].z);
		   }
		   int i = pointlist.length-1;
		   coordinates[i] = new Coordinate(pointlist[i].x, pointlist[i].y, pointlist[i].z);
		   LinearRing lr = gf.createLinearRing(coordinates);
		   Polygon polygon = gf.createPolygon(lr,null);
		   mesh.processDelaunay();
		   
	       for(DTriangle d:mesh.getTriangleList()){
	           if(includeTriangle(d, polygon)){        	   
	        	   if(getNormal(d).equals(new DPoint(normal.x, normal.y, normal.z))){
	        		   triList.add(d);
	        	   } else {
	        		   DTriangle newD = new DTriangle(d.getPoint(0), d.getPoint(2), d.getPoint(1));
	        		   triList.add(newD);
	        	   }
	        	   
	           }
	       }
	   }
   }

   
   public void add(Vertex[] pointlist) throws DelaunayError{
	   add(pointlist,null);
   }
   
   public void clear(){
	   triList.clear();
   }
   
   public void add(ArrayList<DTriangle> aTriList){
	   triList.addAll(aTriList);
   }

   
   
   public String toSTL() throws DelaunayError{
	   StringBuilder sb = new StringBuilder();
	   sb.append("solid poly\n");
	   for(DTriangle tri:triList){
		   sb.append(triToSTL(tri));
	   }
	   sb.append("endsolid vcg\n");
	   return(sb.toString());
   }
    	   
  public String toString(){
	  try {
		return(toSTL());
	} catch (DelaunayError e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return "Error:"+e.getMessage();
	}
	  
  }

			
   private boolean includeTriangle(DTriangle tri, Polygon polygon) throws DelaunayError{
	   boolean include = true;
		for(DEdge e: tri.getEdges()){
			Geometry g = gf.createPoint(e.getMiddle().getCoordinate());
			if(!polygon.contains(g) & !polygon.touches(g)){
				include = false;
			}
			
		}
		return include;

	   
   }
   
   private String triToSTL(DTriangle tri) throws DelaunayError{
	   StringBuilder sb = new StringBuilder();
	   DPoint n = getNormal(tri);
	   sb.append("facet normal  "+n.getX()+" "+n.getY()+" "+n.getZ()+"\n");
	   sb.append("outer loop\n");	
	   for(DPoint p : tri.getPoints()){
		   sb.append("vertex "+p.getX()+" "+p.getY()+" "+p.getZ()+"\n");
	   }
	   sb.append("endloop\n");
	   sb.append("endfacet\n");
	   return sb.toString();
	   
   }
   
   private DPoint getNormal(DTriangle tri) throws DelaunayError{
	   List<DPoint> ps = tri.getPoints();
	   double dx1 = ps.get(1).getX() - ps.get(0).getX();
	   double dx2 = ps.get(2).getX() - ps.get(0).getX();
	   double dy1 = ps.get(1).getY() - ps.get(0).getY();
	   double dy2 = ps.get(2).getY() - ps.get(0).getY();
	   double dz1 = ps.get(1).getZ() - ps.get(0).getZ();
	   double dz2 = ps.get(2).getZ() - ps.get(0).getZ();
	   
	   
	   DPoint vec = new DPoint(dy1*dz2 - dz1*dy2, dz1 * dx2 - dx1 * dz2, dx1 * dy2 - dy1 * dx2);
	   double length = Math.sqrt(vec.squareDistance(new DPoint(0,0,0)));
	   vec.setX(vec.getX()/length);
	   vec.setY(vec.getY()/length);
	   vec.setZ(vec.getZ()/length);
	   return vec;
   }
			
	
   public static void main(String[] args) throws DelaunayError {
		
		
		//mesh.forceConstraintIntegrity();
	   
	   
		try {
			FileWriter outFile = new FileWriter("/home/schalk/tmp/test.stl");
			PrintWriter out = new PrintWriter(outFile);
			ArrayList<Vertex> pointlist = new ArrayList<Vertex>();
			
			TriMesh ptt = new TriMesh();

			pointlist.add(new Vertex(0.0f,0.0f,0.0f));
			pointlist.add(new Vertex(1.0f, 0.0f, 0.0f));
			pointlist.add(new Vertex(1.0f, 1.0f, 0.0f));
			pointlist.add(new Vertex(0.75f, 0.5f, 0.0f));
			pointlist.add(new Vertex(0.5f, 1.0f, 0.0f));
			pointlist.add(new Vertex(0.25f, 0.5f, 0.0f));
			pointlist.add(new Vertex(0.125f, 1.0f, 0.0f));
			pointlist.add(new Vertex(0.0f, 0.5f, 0.0f));
			pointlist.add(new Vertex(0.0f,0.0f,0.0f));
			ptt.add(pointlist.toArray(new Vertex[0]), new Vertex(0,0,-1));

			
			
			pointlist.clear();
			pointlist.add(new Vertex(0.0f,0.0f,0.1f));
			pointlist.add(new Vertex(1.0f, 0.0f, 0.1f));
			pointlist.add(new Vertex(1.0f, 1.0f, 0.1f));
			pointlist.add(new Vertex(0.75f, 0.5f, 0.1f));
			pointlist.add(new Vertex(0.5f, 1.0f, 0.1f));
			pointlist.add(new Vertex(0.25f, 0.5f, 0.1f));
			pointlist.add(new Vertex(0.125f, 1.0f, 0.1f));
			pointlist.add(new Vertex(0.0f, 0.5f, 0.1f));
			pointlist.add(new Vertex(0.0f,0.0f,0.1f));			
			ptt.add(pointlist.toArray(new Vertex[0]), new Vertex(0,0,1));
			
			pointlist.clear();
			pointlist.add(new Vertex(0.0f,0.0f,0.0f));
			pointlist.add(new Vertex(1.0f,0.0f,0.0f));
			pointlist.add(new Vertex(1.0f,0.0f,0.1f));
			pointlist.add(new Vertex(0.0f,0.0f,0.1f));
			pointlist.add(new Vertex(0.0f,0.0f,0.0f));
			ptt.add(pointlist.toArray(new Vertex[0]), null);
			

			out.write(ptt.toSTL());	
			out.close();

		} catch (IOException e){
			e.printStackTrace();
		} 
		
	}
   
   
   
   
}
