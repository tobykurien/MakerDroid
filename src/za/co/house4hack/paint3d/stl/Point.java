package za.co.house4hack.paint3d.stl;

public class Point {
   public float x;
   public float y;
   
   public Point(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public Point(double x, double y) {
      this.x = Float.parseFloat(String.valueOf(x));
      this.y = Float.parseFloat(String.valueOf(y));
   }
   
   public String toString() {
      return "" + x + "," + y;
   }   
}
