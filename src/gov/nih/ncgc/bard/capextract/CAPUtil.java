package gov.nih.ncgc.bard.capextract;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;

public class CAPUtil {

    public static boolean isNumber(String s) {
        try {
            Float.parseFloat(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static float sd(List<Float> values) {
        float s = 0;
        float mean = 0;
        for (Float v : values) mean += v;
        mean /= (float) values.size();

        for (Float v : values) s += (v - mean) * (v - mean);
        s /= (float) (values.size() - 1);
        return (float) Math.sqrt(s);
    }

    public static Connection connectToBARD() throws SQLException {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
//            conn = DriverManager.getConnection("jdbc:mysql://maxwell.nhgri.nih.gov/bard2", "capbard", "bard");
            conn = DriverManager.getConnection("jdbc:mysql://bohr.ncats.nih.gov/bard3", "bard_manager", "bard_manager");
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

    public static Connection connectToBARD(String serverURL) throws SQLException {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
//            conn = DriverManager.getConnection("jdbc:mysql://maxwell.nhgri.nih.gov/bard2", "capbard", "bard");
            conn = DriverManager.getConnection(serverURL, "bard_manager", "bard_manager");
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


    public static boolean insertPublication(Connection conn, String pmid) throws SQLException, IOException, ParsingException {
        // check to see if we already have a pub with this pmid
        PreparedStatement pst = conn.prepareStatement("select * from publication where pmid = " + pmid);
        ResultSet rs = pst.executeQuery();
        boolean exists = false;
        while (rs.next()) exists = true;
        pst.close();
        if (exists) return false;

        String url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=" + pmid + "&rettype=xml";
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        try {
            response = httpClient.execute(get);
        } catch (HttpHostConnectException ex) {
            ex.printStackTrace();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            httpClient = new DefaultHttpClient();
            response = httpClient.execute(get);
        }
        if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 206)
            throw new IOException("Got a HTTP " + response.getStatusLine().getStatusCode() + " for " + url);

        Builder builder = new Builder();
        Document doc = builder.build(response.getEntity().getContent());

        String stitle = null, sabs = null, sdoi = null;

        Nodes title = doc.query("/PubmedArticleSet/PubmedArticle/MedlineCitation/Article/ArticleTitle");
        if (title.size() > 0) stitle = title.get(0).getValue();

        Nodes abs = doc.query("/PubmedArticleSet/PubmedArticle/MedlineCitation/Article/Abstract/AbstractText");
        if (abs.size() > 0) sabs = abs.get(0).getValue();

        Nodes doi = doc.query("/PubmedArticleSet/PubmedArticle/PubmedData/ArticleIdList/ArticleId[idType='doi']");
        if (doi.size() > 0) sdoi = doi.get(0).getValue();

        pst = conn.prepareStatement("insert into publication (pmid, doi, title, abstract) values (?,?,?,?)");
        pst.setInt(1, Integer.parseInt(pmid));
        pst.setString(2, sdoi);
        pst.setString(3, stitle);
        pst.setString(4, sabs);
        pst.execute();
        pst.close();
        return true;
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

    public static CAPDictionary getCAPDictionary()
            throws SQLException, IOException, ClassNotFoundException {
        Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
        PreparedStatement pst = conn.prepareStatement("select dict, ins_date from cap_dict_obj order by ins_date desc");
        try {
            ResultSet rs = pst.executeQuery();
            rs.next();
            byte[] buf = rs.getBytes(1);
            ObjectInputStream objectIn = null;
            if (buf != null)
                objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
            Object o = objectIn.readObject();
            rs.close();
            if (!(o instanceof CAPDictionary)) return null;
            return (CAPDictionary) o;
        } finally {
            pst.close();
        }
    }
}
