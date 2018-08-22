package com.leezai.gisdemo.geotest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Before;
import org.junit.Test;
import org.postgresql.geometric.PGcircle;
import org.postgresql.geometric.PGpoint;

public class GeometricTest {

    private Connection conn;

    @Before
    public void init() throws Exception {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://localhost:5432/gz";
        conn = DriverManager.getConnection(url, "postgres", "postgres");
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TEMP TABLE geomtest(mycirc circle)");
        stmt.close();
    }

    public void destroy() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    public void insertCircle() throws SQLException {

        PGpoint center = new PGpoint(1, 2.5);
        // PGpolygon polygon = new PGpolygon(points);
        double radius = 4;
        PGcircle circle = new PGcircle(center, radius);

        PreparedStatement ps = conn.prepareStatement("INSERT INTO geomtest(mycirc) VALUES (?)");
        ps.setObject(1, circle);
        ps.executeUpdate();
        ps.close();
    }

    @Test
    public void retrieveCircle() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT mycirc, area(mycirc) FROM geomtest");
        rs.next();
        PGcircle circle = (PGcircle) rs.getObject(1);
        double area = rs.getDouble(2);
        // PG

        PGpoint center = circle.center;
        double radius = circle.radius;

        System.out.println("Center (X, Y) = (" + center.x + ", " + center.y + ")");
        System.out.println("Radius = " + radius);
        System.out.println("Area = " + area);
    }

    @Test
    public void getWKTFromGEOM() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT ST_AsText(geom) FROM sheng where name = '新疆维吾尔自治区'");
        rs.next();
        String wkt = rs.getString(1);
        System.out.println(wkt);
    }

    @Test
    public void getWKBFromGEOM() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT ST_AsBinary(geom) FROM sheng");
        rs.next();
        String wkt = rs.getString(1);
        System.out.println("wkt = " + wkt);
    }

    @Test
    public void getEWKTFromGEOM() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT ST_AsEWKT(geom) FROM sheng");
        rs.next();
        String wkt = rs.getString(1);
        System.out.println("wkt = " + wkt);
    }

    @Test
    public void getBufferFromGEOM() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT ST_AsEWKT(ST_Buffer(geom, 0.2)), ST_AsEWKT(geom) FROM sheng where id = 1");
        rs.next();
        String wkt = rs.getString(1);
        String wkt2 = rs.getString(2);
        //System.out.println("tt " + wkt.substring(0, 100));
        //System.out.println("tt " + wkt2.substring(0, 100));
    }

    /**
     * 需要在postGis中模拟一个dual表（注意指明空间字段）
     */
    @Test
    public void getBufferFromWKT() throws SQLException {
        // 新疆维吾尔自治区
        String wkt ="";
        //PropertiesUtility.getInstance().findFileValue("system.properties", "wkt");
        long startTime = new java.util.Date().getTime();
        System.out.println(startTime);
        String sql = "SELECT ST_AsText(ST_Buffer(st_geomfromtext('" + wkt + "'), 0.2)) FROM dual";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        rs.next();

        long endTime = new java.util.Date().getTime();
        System.out.println(endTime);
        System.out.println("缓冲时间：" + (endTime - startTime));
        System.out.println("坐标点数：" + wkt.split(",").length);
    }

    /**
     * 根据指定的缓冲距离进行缓冲，以米为单位
     */
    @Test
    public void getBufferFromWKT2() throws SQLException {
        // 新疆维吾尔自治区  wkt=MULTIPOLYGON(((79.036744 34.3364.............
        String wkt ="";
        //PropertiesUtility.getInstance().findFileValue("system.properties", "wkt");
        long startTime = new java.util.Date().getTime();
        System.out.println(startTime);
        // 缓冲距离为10.8KM
        String sql = "SELECT ST_AsText(st_transform(st_setsrid(ST_Buffer(st_transform(st_setsrid(st_geomfromtext('" + wkt
                + "'), 4326), 2333), 10800), 2333), 4326)) FROM dual";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        rs.next();
        String resultWKT = rs.getString(1);

        System.out.println(resultWKT);

        long endTime = new java.util.Date().getTime();
        System.out.println(endTime);
        System.out.println("缓冲时间：" + (endTime - startTime));
        System.out.println("坐标点数：" + wkt.split(",").length);
    }

    // 判断点是否在多边形内
    @Test
    public void getWithin() throws SQLException {
        Statement stmt = conn.createStatement();
        String sql = "SELECT name FROM sheng where ST_Within(ST_MakePoint(116.561, 40.276), geom)";
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            String wkt = rs.getString(1);
            System.out.println(wkt);
        }
    }
}