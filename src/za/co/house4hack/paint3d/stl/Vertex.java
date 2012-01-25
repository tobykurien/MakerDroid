package za.co.house4hack.paint3d.stl;

public class Vertex {
   public float x;
   public float y;
   public float z;
   
   public Vertex(float x, float y, float z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public String toString() {
      return "" + x + "," + y + "," + z;
   }
}
