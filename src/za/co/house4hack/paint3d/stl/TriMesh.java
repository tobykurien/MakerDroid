package za.co.house4hack.paint3d.stl;

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


import android.text.InputFilter.LengthFilter;

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
    * Convert list of polygons into a list of lists of DTriangles
    * @param pointlist - contains list of counter clockwise list of vertices
    * @param normal - used for complex polygons to indicate the normal
    * @throws DelaunayError
    */
   
   public List<List<DTriangle>> build(List<Vertex[]> polyList, Vertex normal) throws DelaunayError{
	   ArrayList<List<DTriangle>> resultList = new ArrayList<List<DTriangle>>();
	   if(polyList.get(0).length==5 & polyList.size()==1){ //Simple case 
		   Vertex[] pointlist = polyList.get(0);
		   ArrayList<DTriangle> aTriList = new ArrayList<DTriangle>();
		   DPoint p1 = new DPoint(pointlist[0].x,pointlist[0].y, pointlist[0].z);
		   DPoint p2 = new DPoint(pointlist[1].x,pointlist[1].y, pointlist[1].z);
		   DPoint p3 = new DPoint(pointlist[2].x,pointlist[2].y, pointlist[2].z);
		   aTriList.add(new DTriangle(p1,p2,p3));

		   p1 = new DPoint(pointlist[2].x,pointlist[2].y, pointlist[2].z);
		   p2 = new DPoint(pointlist[3].x,pointlist[3].y, pointlist[3].z);
		   p3 = new DPoint(pointlist[4].x,pointlist[4].y, pointlist[4].z);
		   aTriList.add(new DTriangle(p1,p2,p3));
		   resultList.add(aTriList);
		   
	   } else {
		   
		   ConstrainedMesh mesh = new ConstrainedMesh();
		   Polygon[] polygonArr = new Polygon[polyList.size()]; 

		   
		   for(int k=0; k<polyList.size(); k++){
			   Vertex[] pointlist = polyList.get(k);
			   Coordinate coordinates[] = new Coordinate[pointlist.length];
			   for(int i=0; i<pointlist.length-1; i++){
				   int j = i+1;
					mesh.addConstraintEdge(new DEdge(pointlist[i].x, pointlist[i].y, pointlist[i].z,pointlist[j].x, pointlist[j].y, pointlist[j].z));			
					coordinates[i] = new Coordinate(pointlist[i].x, pointlist[i].y, pointlist[i].z);
			   }		   		   
			   int i = pointlist.length-1;
			   mesh.addConstraintEdge(new DEdge(pointlist[i].x, pointlist[i].y, pointlist[i].z,pointlist[0].x, pointlist[0].y, pointlist[0].z));						   
			   coordinates[i] = new Coordinate(pointlist[i].x, pointlist[i].y, pointlist[i].z);
			   LinearRing lr = gf.createLinearRing(coordinates);
			   polygonArr[k] = gf.createPolygon(lr,null);
			   resultList.add(new ArrayList<DTriangle>());
		   }
	   
		   
		   
		   mesh.processDelaunay();
		   
		   // Sort triangles into lists based on whether they are inside
		   for(DTriangle d:mesh.getTriangleList()){			   
				List<DTriangle> aTriList;
				for(int p=polygonArr.length-1; p>=0; p--){		   
					   Polygon aPolygon = polygonArr[p];
		    	       if(insidePolygon(d, aPolygon)){
		    	    	   aTriList = resultList.get(p);   
		    	       
			        	   if(getNormal(d).equals(new DPoint(normal.x, normal.y, normal.z))){
			        		   aTriList.add(d);
			        	   } else {
			        		   DTriangle newD = new DTriangle(d.getPoint(0), d.getPoint(2), d.getPoint(1));
			        		   aTriList.add(newD);
			        	   }
			        	   break;
		    	       }
		        	   
		         }
	       }
	       
	       
	   }
	   return resultList;
   }

   
   public void add(Vertex[] pointlist) throws DelaunayError{
	   add(pointlist,null);
   }
   
   public void add(Vertex[] pointlist, Vertex normal) throws DelaunayError{
	   ArrayList<Vertex[]> l = new ArrayList<Vertex[]>();
	   l.add(pointlist);
	   List<List<DTriangle>> resultlist = build(l, normal);
	   add(resultlist.get(0));
   }
   
   
   public void clear(){
	   triList.clear();
   }
   
   public void add(List<DTriangle> aTriList){
	   triList.addAll(aTriList);
   }

   public void add(DTriangle d) throws DelaunayError{
	   triList.add(d);
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
		e.printStackTrace();
		return "Error:"+e.getMessage();
	}
	  
  }

			
   private boolean insidePolygon(DTriangle tri, Polygon polygon) throws DelaunayError{
	   boolean isInside = true;
		for(DEdge e: tri.getEdges()){
			Geometry g = gf.createPoint(e.getMiddle().getCoordinate());
			if(!polygon.contains(g) & !polygon.touches(g)){
				isInside = false;
			}
			
		}
		return isInside;

	   
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
			TriMesh ptt = new TriMesh();
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
	         ArrayList<Vertex> hole1 = new ArrayList<Vertex>();
	         hole1.add(new Vertex(1.0f, 1.0f, 0.0f));
	         hole1.add(new Vertex(2.0f, 1.0f, 0.0f));
	         hole1.add(new Vertex(2.0f, 2.0f, 0.0f));
	         hole1.add(new Vertex(1.0f, 2.0f, 0.0f));
	         hole1.add(new Vertex(1.0f, 1.0f, 0.0f));

	         ArrayList<Vertex[]> holeList = new ArrayList<Vertex[]>();
	         holeList.add(hole1.toArray(new Vertex[0]));	
	         
	         ArrayList polyList = new ArrayList<Vertex[]>();
	         polyList.add(pointlist.toArray(new Vertex[0]));
	         polyList.add(hole1.toArray(new Vertex[0]));
	         
	         List<List<DTriangle>> result = ptt.build(polyList,  new Vertex(0, 0, 1));
	         ptt.add(result.get(0));

			out.write(ptt.toSTL());	
			out.close();

		} catch (IOException e){
			e.printStackTrace();
		} 
		
	}
   
   
   
   
}
