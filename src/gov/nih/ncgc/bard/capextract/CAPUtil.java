package gov.nih.ncgc.bard.capextract;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class CAPUtil {

    public static Connection connectToBARD() throws SQLException {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection("jdbc:mysql://BOGUS.nhgri.nih.gov/bard2", "bardcap", "bard");
            conn.setAutoCommit(false);
        } catch (IllegalAccessException e) {
            System.out.println("Can't connect to db" + e.toString());
            System.exit(-1);
        } catch (ClassNotFoundException e) {
            System.out.println("Can't connect to db" + e.toString());
            System.exit(-1);
        } catch (InstantiationException e) {
            System.out.println("Can't connect to db" + e.toString());
            System.exit(-1);
        } catch (SQLException e) {
            System.out.println("Can't connect to db" + e.toString());
            System.exit(-1);
        }
        return conn;
    }

    static public void jaxbString(Object obj, Vector<String> levels) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method[] methods = obj.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterTypes().length == 0 && method.getName().length() > 3 && method.getName().startsWith("get")) {
                Object out = method.invoke(obj, new Object[0]);
                if (out instanceof ArrayList) {
                    ArrayList<?> al = (ArrayList<?>) out;
                    if (al.size() == 0)
                        out = null;
                    else if (al.size() == 1)
                        out = al.get(0);
                    //else break;
                }
                if (out != null) {
                    levels.add(method.getName().substring(3));
                    if (out instanceof ArrayList) {
                        //System.out.println("LIST:");
                        for (Iterator<?> it = ((ArrayList<?>) out).iterator(); it.hasNext(); ) {
                            Object item = it.next();
                            //System.out.println("LISTITEM");
                            if (item.getClass().toString().contains(".jaxb."))
                                jaxbString(item, levels);
                            else
                                System.out.println(levels.toString() + "|" + item.getClass().toString() + " - " + item.toString());
                        }
                    } else if (out.getClass().toString().contains(".jaxb."))
                        jaxbString(out, levels);
                    else
                        System.out.println(levels.toString() + "|" + out.getClass().toString() + " - " + out.toString());
                    levels.remove(levels.size() - 1);
                }
            }
        }

        return;
    }

    static public HashMap<String, Object> jaxbHashMap(Object obj) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        HashMap<String, Object> data = new HashMap<String, Object>();
        Method[] methods = obj.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterTypes().length == 0 && method.getName().length() > 3 && method.getName().startsWith("get")) {
                //if (method.getReturnType().isAssignableFrom(List.class))
                data.put(method.getName(), method.invoke(obj));
            }
        }
        return data;
    }

    static public <T extends Object> T jaxbConstructor(Class<T> clazz, HashMap<String, Object> data) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<T> constructor = clazz.getConstructor();
        T obj = constructor.newInstance();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterTypes().length == 0 && method.getName().length() > 3 && method.getName().startsWith("get")) {
                if (method.getReturnType().isAssignableFrom(List.class)) {
                    List<?> value = (List<?>) data.get(method.getName());
                    if (value != null) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) method.invoke(obj);
                        for (Object entry : value) list.add(entry);
                    }
                }
            }
            if (method.getParameterTypes().length == 1 && method.getName().length() > 3 && method.getName().startsWith("set")) {
                String methodName = "g" + method.getName().substring(1);
                Object value = data.get(methodName);
                if (value != null)
                    method.invoke(obj, value);
            }
        }
        return obj;
    }
}
