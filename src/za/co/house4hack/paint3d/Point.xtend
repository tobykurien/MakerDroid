package za.co.house4hack.paint3d

class Point {
	public float x
	public float y
	
	new (float x, float y) {
	   this.x = x
	   this.y = y
	}
	
	override toString() {
	   "" + x + "," + y
	}
}