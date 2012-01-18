package za.co.house4hack.paint3d.stl

class Vertex {
	float x
	float y
	float z
	
	new(float x, float y, float z) {
	   this.x = x
	   this.y = y
	   this.z = z
	}

   override toString() {
      "" + x + "," + y + "," + z
   }
}