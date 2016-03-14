package com.homer.util.data;

import com.homer.util.core.IBaseObject;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Table;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;

/**
 * Created by arigolub on 2/14/16.
 */
public class Connector {

    private static Connection getConnection() throws SQLException {
        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", "root");
        connectionProps.put("password", "");

        conn = DriverManager.getConnection(
                "jdbc:mysql://localhost/homer3",
                connectionProps);
        conn.setAutoCommit(false);
        System.out.println("Connected to database");
        return conn;
    }

    private static String join(List objs)
    {
        StringBuilder sb = new StringBuilder("");
        for(int i = 0; i < objs.size(); i++)
        {
            sb.append(" ").append(objs.get(i)).append(" ");
            if (i < objs.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private static List<Field> fields(Class baseObject)
    {
        List<Field> fields = new ArrayList<>();
        Class clazz = baseObject;
        while(clazz.getSuperclass() != null) {
            for(Field f : clazz.getDeclaredFields()) {
                fields.add(f);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private static <T extends IBaseObject> String buildQuery(Class<T> baseObject, Map<String, ?> filters)
    {
        String tableName = baseObject.getAnnotation(Table.class).name();
        StringBuilder query = new StringBuilder("SELECT ");
        Class clazz = baseObject;
        List<String> columns = new ArrayList<String>();
        List<Field> fields = fields(baseObject);
        for(Field f : fields) {
            if (f.isAnnotationPresent(Column.class)) {
                columns.add(f.getName());
            }
        }
        query.append(join(columns));
        query.append("\n FROM ").append(tableName);
        if (filters.size() > 0) {
            query.append("\n WHERE ");
            Iterator<String> iterator = filters.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                query.append(key);
                if (List.class.isAssignableFrom(filters.get(key).getClass())) {
                    query.append(" in ").append("(");
                    List objects = (List)filters.get(key);
                    query.append(join(objects));
                    query.append(")");
                } else {
                    query.append(" = '").append(filters.get(key)).append("'");
                }
                if (iterator.hasNext()) {
                    query.append("\n AND ");
                }
            }
        }
        query.append(";");
        return query.toString();
    }

    private static <T extends IBaseObject> String buildUpsert(T baseObject) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<T> clazz = (Class<T>) baseObject.getClass();
        String tableName = clazz.getAnnotation(Table.class).name();
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(tableName).append(" (");
        List<String> columns = new ArrayList<>();
        List<Field> fields = fields(clazz);
        for(Field f : fields) {
            if (f.isAnnotationPresent(Column.class)) {
                columns.add(f.getName());
            }
        }
        query.append(join(columns));
        query.append(")").append(" VALUES ").append(" ( ");
        for(int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            if (f.isAnnotationPresent(Column.class)) {
                Object obj = PropertyUtils.getProperty(baseObject, f.getName());
                if (obj == null) {
                    query.append("null");
                } else if(String.class.isAssignableFrom(obj.getClass())) {
                    query.append("'").append(obj.toString()).append("'");
                } else if (DateTime.class.isAssignableFrom(obj.getClass())) {
                    query.append(((DateTime) obj).getMillis());
                } else if ("boolean".equals(obj.getClass().getName())) {
                    query.append(((boolean)obj) == true ? 1 : 0);
                } else {
                    query.append(obj.toString());
                }
                if (i < fields.size() - 1) {
                    query.append(" , ");
                }
            }
        }
        query.append(" ) ");
        query.append(" ON DUPLICATE KEY UPDATE ");
        for(int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            if (f.isAnnotationPresent(Column.class)) {
                String columnName  = f.getName();
                Object obj = PropertyUtils.getProperty(baseObject, f.getName());
                if (obj == null) {
                    query.append(columnName).append(" = ").append("null");
                } else if(String.class.isAssignableFrom(obj.getClass())) {
                    query.append(columnName).append(" = ").append("'").append(obj.toString()).append("'");
                } else if (DateTime.class.isAssignableFrom(obj.getClass())) {
                    query.append(columnName).append(" = ").append(((DateTime)obj).getMillis());
                } else if ("boolean".equals(obj.getClass().getName())) {
                    query.append(columnName).append(" = ").append(((boolean)obj) == true ? 1 : 0);
                } else {
                    query.append(columnName).append(" = ").append(obj.toString());
                }
                if (i < fields.size() - 1) {
                    query.append(" , ");
                }
            }
        }
        query.append(";");
        return query.toString();
    }

    private static <T extends IBaseObject> List<T> hydrate(Class<T> baseObject, ResultSet rs) throws SQLException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        List<T> results = new ArrayList<>();
        List<Field> fields = fields(baseObject);
        while(rs.next()) {
            T obj = baseObject.newInstance();
            for(Field f : fields) {
                String fieldName = f.getName();
                if (f.getType().isPrimitive()) {
                    if("long".equals(f.getType().getName())) {
                        BeanUtils.setProperty(obj, fieldName, rs.getLong(fieldName));
                    } else if ("int".equals(f.getType().getName())) {
                        BeanUtils.setProperty(obj, fieldName, rs.getInt(fieldName));
                    } else if ("boolean".equals(f.getType().getName())) {
                        BeanUtils.setProperty(obj, fieldName, rs.getBoolean(fieldName));
                    }
                } else if (Integer.class.equals(f.getType())) {
                    Integer o = rs.getInt(fieldName);
                    if (rs.wasNull()) {
                        o = null;
                    }
                    PropertyUtils.setProperty(obj, fieldName, o);
                } else if (Long.class.equals(f.getType())) {
                    Long o = rs.getLong(fieldName);
                    if(rs.wasNull()) {
                        o = null;
                    }
                    PropertyUtils.setProperty(obj, fieldName, o);
                } else if (String.class.equals(f.getType())) {
                    BeanUtils.setProperty(obj, fieldName, rs.getString(fieldName));
                } else if (DateTime.class.equals(f.getType())) {
                    long utcSeconds = 1000 * rs.getLong(fieldName);
                    BeanUtils.setProperty(obj, fieldName, new DateTime(utcSeconds));
                }
            }
            results.add(obj);
        }
        return results;
    }

    public static <T extends IBaseObject> List<T> get(Class<T> baseObject, Map<String, ?> filters) {
        List<T> results = null;
        Statement stmt = null;
        String query = buildQuery(baseObject, filters);
        try {
            System.out.println(query);
            Connection con = getConnection();
            stmt = con.createStatement();
            results = hydrate(baseObject, stmt.executeQuery(query));
        } catch (SQLException e ) {
            //do something
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return results;
    }

    public static <T extends IBaseObject> T upsert(T object) throws SQLException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Statement stmt = null;
        String query = buildUpsert(object);
        Connection con = null;
        try {
            System.out.println(query);
            con = getConnection();
            stmt = con.createStatement();
            int rowCount = stmt.executeUpdate(query);
            if (rowCount == 0) {
                //didn't work
            }
            con.commit();
            ResultSet rs = stmt.getGeneratedKeys();
            if(rs.next()) {
                object.setId(rs.getLong(1));
            }
        } catch (Exception e) {
            if (con != null) {
                con.rollback();
            }
        } finally {
            if (stmt != null) { stmt.close(); }
        }
        return object;
    }
}
