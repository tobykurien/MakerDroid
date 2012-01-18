package za.co.house4hack.paint3d.stl

/**
 * Take a flat polygon and extrude it into 3 dimensions and decompose into triangles
 */
import java.util.List
import za.co.house4hack.paint3d.Point

class Extruder {
	List<Point> boundary
	
	new(List<Point> boundary) {
	   this.boundary = boundary
	}
	
	// The method to extrude the polygon data into a 3d object
	def Object3d extrude() {
	   
	} 
	
}