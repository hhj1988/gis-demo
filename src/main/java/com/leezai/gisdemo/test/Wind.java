package com.leezai.gisdemo.test;

import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class Wind {
    private Connection conn = null;

    public Connection getConn() {
        if (conn == null) {
            try {
                Class.forName("org.postgresql.Driver");
                String url = "jdbc:postgresql://localhost:5432/gz";
                conn = DriverManager.getConnection(url, "postgres", "postgres");
                conn.setAutoCommit(false);
            } catch (Exception e) {
                System.err.print(e);
            }
        }

        return conn;
    }

    public ArrayList getWinds() {
        ArrayList  result = new ArrayList ();

        if (this.getConn() == null)
            return result;

        try {
            String sql = "select *,ST_AsGeoJson(shape) from sde.wind";

            Statement st = this.getConn().createStatement();
            st.setFetchSize(0);
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                HashMap map = new HashMap();
                map.put("shape", rs.getString("ST_AsGeoJson"));
                map.put("velocity", rs.getString("velocity"));
                map.put("direction", rs.getString("direction"));
                result.add(map);
            }
            rs.close();
            st.close();
        } catch (Exception e) {
            System.err.print(e);
        }

        return result;
    }


    public ArrayList getEffectZones() {
        ArrayList  result = new ArrayList();

        if (this.getConn() == null)
            return result;

        try {
            String sql = "select *,ST_AsGeoJson(";
            sql += "ST_Buffer(";
            sql += "ST_PolygonFromText(";
            sql += "'POLYGON(('";
            sql += "||ST_X(shape)||' '||ST_Y(shape)||','";
            sql += "||ST_X(shape)+velocity*cos((direction+15)*PI()/180)/20||' '||ST_Y(shape)+velocity*sin((direction+15)*PI()/180)/20||','";
            sql += "||ST_X(shape)+velocity*cos((direction-15)*PI()/180)/20||' '||ST_Y(shape)+velocity*sin((direction-15)*PI()/180)/20||','";
            sql += "||ST_X(shape)||' '||ST_Y(shape)||'))'";
            sql += ")";
            sql += ", velocity/50";
            sql += ")";
            sql += ") ";
            sql += "from sde.wind";

            Statement st = this.getConn().createStatement();
            st.setFetchSize(0);
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                HashMap map = new HashMap();
                map.put("shape", rs.getString("ST_AsGeoJson"));
                map.put("velocity", rs.getString("velocity"));
                map.put("direction", rs.getString("direction"));
                result.add(map);
            }
            rs.close();
            st.close();
        } catch (Exception e) {
            System.err.print(e);
        }

        return result;
    }


}