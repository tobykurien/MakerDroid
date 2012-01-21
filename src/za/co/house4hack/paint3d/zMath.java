package za.co.house4hack.paint3d;

import za.co.house4hack.paint3d.stl.Vertex;

public class zMath {
/**
   public static double DistanceToLine(int lineStartX, int lineStartY, int lineEndX, int lineEndY, int pointX, int pointY) {
      return DistanceToLine(lineStartX, lineStartY, lineEndX, lineEndY, pointX, (double) pointY);
   }

   public static double DistanceToLine(double lineStartX, double lineStartY, double lineEndX, double lineEndY, double pointX, double pointY) {
      double distance = 0;
      Vertex lineStart = new Vertex((float) lineStartX, (float) lineStartY, 1f);
      Vertex lineEnd = new Vertex((float) lineEndX, (float) lineEndY, 1);
      Vertex point = new Vertex((float) pointX, (float) pointY, 1);

      int isValid = distancePointLine(point, lineStart, lineEnd, distance);

      if (isValid == 0) { return -1; }
      return distance;
   }

   private static int distancePointLine(Vertex Point, Vertex LineStart, Vertex LineEnd, double distance) {
      double LineMag;
      double U;
      Vertex Intersection = new Vertex(0,0,0);

      LineMag = Magnitude(LineEnd, LineStart);

      U = (((Point.X - LineStart.X) * (LineEnd.X - LineStart.X)) + ((Point.Y - LineStart.Y) * (LineEnd.Y - LineStart.Y)) + ((Point.Z - LineStart.Z) * (LineEnd.Z - LineStart.Z)))
               / (LineMag * LineMag);

      if (U < 0.0f || U > 1.0f) return 0; // closest point does not fall within
                                          // the line segment

      Intersection.X = LineStart.X + U * (LineEnd.X - LineStart.X);
      Intersection.Y = LineStart.Y + U * (LineEnd.Y - LineStart.Y);
      Intersection.Z = LineStart.Z + U * (LineEnd.Z - LineStart.Z);

      distance = Magnitude(Point, Intersection);

      return 1;
   }

   public static double Magnitude(Vertex Point1, Vertex Point2) {
      Vertex vector = new Vertex(0,0,0);

      vector.X = Point2.X - Point1.X;
      vector.Y = Point2.Y - Point1.Y;
      vector.Z = Point2.Z - Point1.Z;

      return (double) Math.Sqrt(vector.X * vector.X + vector.Y * vector.Y + vector.Z * vector.Z);
   }

   public static double DistanceBetweenPoints(float x1, float y1, float x2, float y2) {
      Vertex vector = new Vertex();

      vector.X = x2 - x1;
      vector.Y = y2 - y1;
      vector.Z = 0;

      return (double) Math.Sqrt(vector.X * vector.X + vector.Y * vector.Y + vector.Z * vector.Z);
   }

   public static boolean IsInPolygon(double[] listPointsX, double[] listPointsY, double x, double y) {
      return pointInPolygon(listPointsX.Length, listPointsX, listPointsY, x, y);
   }

   public static boolean IsInRectangle(double centerX, double centerY, double width, double height, double x, double y) {
      double leftBorder = centerX - (width / 2);
      double rightBorder = centerX + (width / 2);

      double topBorder = centerY - (height / 2);
      double bottomBorder = centerY + (height / 2);

      boolean isInX = x > leftBorder && x < rightBorder;
      boolean isInY = y > topBorder && y < bottomBorder;

      // Console.WriteLine("x from right border: " +
      // Convert.ToString(rightBorder - x));
      return isInX && isInY;
   }

   public static boolean IsInCircle(int centerX, int centerY, double radius, int x, int y) {
      double distance = zConvert.GetDistanceBetweenCoords(centerX, centerY, x, y);

      // Console.WriteLine("Distance between point and circle center: " +
      // distance + ". Circle radius: " + radius);

      return distance < radius;
   }

   private static boolean pointInPolygon(int polySides, double[] polyX, double[] polyY, double x, double y) {
      int i, j = 0;
      boolean oddNODES = false;

      for (i = 0; i < polySides; i++) {
         j++;
         if (j == polySides) j = 0;
         if (polyY[i] < y && polyY[j] >= y || polyY[j] < y && polyY[i] >= y) {
            if (polyX[i] + (y - polyY[i]) / (polyY[j] - polyY[i]) * (polyX[j] - polyX[i]) < x) {
               oddNODES = !oddNODES;
            }
         }
      }

      return oddNODES;
   }

   public static boolean IsInEllipse(int centerX, int centerY, double width, double height, int xPoint, int yPoint) {
      xPoint = xPoint - centerX; // formula assumes ellipse at 0,0
      yPoint = yPoint - centerY; // formula assumes ellipse at 0,0

      double x = width * height * xPoint / Math.sqrt(Math.pow(height * xPoint, 2) + Math.pow(width * yPoint, 2));
      double y = width * height * yPoint / Math.sqrt(Math.pow(height * xPoint, 2) + Math.pow(width * yPoint, 2));

      xPoint = xPoint * 2; // (* 2) not sure why, but seems to work
      yPoint = yPoint * 2; // (* 2) not sure why, but seems to work

      // Console.WriteLine("x point: " + " " + xPoint + " y point: " + yPoint);
      // Console.WriteLine("x: " + " " + x + " y: " + y);

      boolean isInX = Math.abs(xPoint) < Math.abs(x);
      boolean isInY = Math.abs(yPoint) < Math.abs(y);

      // double distance = getDistance(xPoint, x,yPoint,y);
      // Console.WriteLine("Distance between point and ellipse intersection: " +
      // distance);

      return isInX && isInY;
   }

   private static double getDistance(double x1, double x2, double y1, double y2) {
      double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

      return distance;
   }
   **/
}
