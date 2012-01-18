package za.co.house4hack.paint3d.stl

/**
 * Take a flat polygon and extrude it into 3 dimensions and decompose into triangles
 */
import java.util.List
import za.co.house4hack.paint3d.Point

class Extruder {
	List<Point> boundary
   List<List<Point>> etching
	
	/**
	 * @boundary - polygon representing the outside boundary of the object
	 * @etching - list of polygons representing sunken surfaces on the top face of object, in order of depth 
	 */
	new(List<Point> boundary, List<List<Point>> etching) {
	   this.boundary = boundary
	   this.etching = etching
	}
	
	// The method to extrude the polygon data into a 3d object
	def Object3d extrude() {
	   var result = new Object3d()

      // TODO - decompose boundary into triangles
      // then copy the point cloud to create the top face
      // then decompose the etching polygons into triangles
      // then insert the point cloud for the etching into the top face
      // then copy the point cloud for each etching polygon and drop below top face in increasing depth
      // then define all the faces of the object
      // finally copy all this data into the result object
	   
	   result
	} 
	
}