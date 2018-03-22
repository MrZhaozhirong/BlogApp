package org.zzrblog.blogapp.utils;



/**
 * Created by zzr on 2018/2/7.
 */

public class Geometry {

    /**
     * 几何点
     */
    public static class Point {
        public final float x,y,z;
        public Point(float x,float y,float z){
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public Point translateX(float value) {
            return new Point(x,y+value,z);
        }
        public Point translateY(float value) {
            return new Point(x,y+value,z);
        }
        public Point translateZ(float value) {
            return new Point(x,y+value,z);
        }
        public Point translate(Vector vector) {
            return new Point(x+vector.x,y+vector.y,z+vector.z);
        }
    }

    /**
     * 圆形 = 中心点+半径
     */
    public static class Circle{
        public final Point center;
        public final float radius;

        public Circle(Point center, float radius){
            this.center = center;
            this.radius = radius;
        }

        public Circle scale(float scale){
            return new Circle(center, radius * scale);
        }
    }

    /**
     * 圆柱
     */
    public static class Cylinder{
        public final Point center;
        public final float radius;
        public final float height;

        public Cylinder(Point center, float radius, float height){
            this.center = center;
            this.radius = radius;
            this.height = height;
        }
    }

    /**
     * 球 = 球心点 + 半径
     */
    public static class Sphere {
        public final Point center;
        public final float radius;

        public Sphere(Point center, float radius){
            this.center = center;
            this.radius = radius;
        }
    }

    /**
     * 方向 向量
     */
    public static class Vector {
        public final float x,y,z;
        public Vector(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public float length() {
            return (float) Math.sqrt(x*x + y*y + z*z);
        }
        public Vector crossProduct(Vector other) {
            return new Vector(
                    (y*other.z) - (z*other.y),
                    (x*other.z) - (z*other.x),
                    (x*other.y) - (y*other.x)
            );
        }
        public float dotProduct(Vector other) {
            return x * other.x +
                    y * other.y +
                    z * other.z ;
        }
        public Vector scale(float f) {
            return new Vector(x*f, y*f, z*f);
        }
    }

    /**
     * 射线 = 起点 + 方向向量
     */
    public static class Ray {
        public final Point point;
        public final Vector vector;

        public Ray(Point point, Vector vector) {
            this.point = point;
            this.vector = vector;
        }
    }

    /**
     * 平面 = 平面切点 + 法向量
     */
    public static class Plane {
        public final Point point;
        public final Vector normal;

        public Plane(Point point,Vector normal) {
            this.point = point;
            this.normal = normal;
        }
    }

    /**
     * 向量相减 A-B = BA
     * @param from A
     * @param to B
     * @return BA
     */
    public static Vector vectorBetween(Point from, Point to) {
        return new Vector(
                to.x-from.x,
                to.y-from.y,
                to.z-from.z);
    }

    // 包围球 与 射线 的相交测试接口
    public static boolean intersects(Ray ray, Sphere sphere) {
        return sphere.radius > distanceBetween(sphere.center, ray);
    }
    // 求出包围球中心点 与 射线的距离
    public static float distanceBetween(Point centerPoint, Ray ray) {
        // 第一个向量：开始点到球心
        Vector vStart2Center = vectorBetween(ray.point, centerPoint);
        // 第二个向量：结束点到球心
        Vector vEnd2Center = vectorBetween(
                ray.point.translate(ray.vector), // 结束点 = 开始点 + 方向向量。
                centerPoint);
        // 两个向量的叉积
        Vector crossProduct = vStart2Center.crossProduct(vEnd2Center);
        // 两个向量叉积的值大小 = 三角形面积 * 2
        float areaOf2 = crossProduct.length();
        // 求出射线的长度 = 三角形的底边长
        float lengthOfRay = ray.vector.length();
        // 高 = 面积*2 / 底边长
        float distanceFromSphereCenterToRay = areaOf2 / lengthOfRay;
        return distanceFromSphereCenterToRay;
    }
    // 射线 与 平面 相交点
    public static Point intersectionPoint(Ray ray, Plane tablePlane) {
        // 先找出 射点 到 平面点的 向量
        Vector rayToPlaneVector = vectorBetween(ray.point, tablePlane.point);
        // 然后就是 射点到平面点的向量 与 平面法向量的点积 / 射线方向向量 与 平面法向量的点积
        float scaleFactor = rayToPlaneVector.dotProduct(tablePlane.normal) / ray.vector.dotProduct(tablePlane.normal);
        // 根据缩放因子，缩放射线向量，射线平面交点 = 射点 + 缩放后的方向向量
        Point intersctionPoint = ray.point.translate(ray.vector.scale(scaleFactor));
        return intersctionPoint;
    }


}
