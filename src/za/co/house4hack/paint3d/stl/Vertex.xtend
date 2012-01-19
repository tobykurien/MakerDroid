package za.co.house4hack.paint3d.stl

class Vertex {
	public float x
	public float y
	public float z
	
	new(float x, float y, float z) {
	   this.x = x
	   this.y = y
	   this.z = z
	}

   override toString() {
      "" + x + "," + y + "," + z
   }
}