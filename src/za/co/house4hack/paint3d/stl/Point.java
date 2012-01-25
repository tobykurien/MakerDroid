package za.co.house4hack.paint3d.stl;

public class Point {
   public float x;
   public float y;
   
   public Point(float x, float y) {
      this.x = x;
      this.y = y;
   }
   
   public String toString() {
      return "" + x + "," + y;
   }   
}
