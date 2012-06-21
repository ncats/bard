package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.jaxb.Dictionary;
import gov.nih.ncgc.bard.capextract.jaxb.Experiments;
import gov.nih.ncgc.bard.capextract.jaxb.Link;
import gov.nih.ncgc.bard.capextract.jaxb.Project;
import gov.nih.ncgc.bard.capextract.jaxb.Projects;
import gov.nih.ncgc.bard.capextract.jaxb.Result;
import gov.nih.ncgc.bard.capextract.jaxb.Results;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.bind.util.ValidationEventCollector;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * Example code to play with the Broad CAP Data Export API.
 * CAP data export API defined at https://github.com/broadinstitute/BARD/wiki/BARD-Data-Export-API
 * @author Rajarshi Guha
 */
public class CAPExtractor {
    public static final String EXPORT_URL = "http://bard.nih.gov/bardexport";

    protected Client CAPclient;
    protected Unmarshaller unmarshaller;
    protected ValidationEventCollector CAPvec;
    
    protected HashMap<String,Object> dict;
    
    protected enum CAPresource {
	Dictionary ("resources/test/dictionary.xml", true),
	Projects ("resources/test/projects.xml", true),
	Experiments ("resources/test/experiments.xml", true),
	Results ("resources/test/results.xml", true);
	
	public final String uri;
	public boolean isfile;
	CAPresource(String uri, boolean isfile) {
	    this.uri = uri;
	    this.isfile = isfile;
	}
    }
    
    public CAPExtractor() throws JAXBException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
	CAPclient = new Client();
	JAXBContext jc = JAXBContext.newInstance("gov.nih.ncgc.bard.capextract.jaxb");
	unmarshaller = jc.createUnmarshaller();
	CAPvec = new ValidationEventCollector();
	unmarshaller.setEventHandler(CAPvec);

	getDictionary();
    }

    private Reader getResource(String uri, boolean file) {
	if (file) { 
	    try {
		return new FileReader(new File(uri));
	    } catch (Exception ex) {ex.printStackTrace(); return null;}
	}
	
	WebResource r = CAPclient.resource(uri);
	r.accept(MediaType.APPLICATION_XML_TYPE);
	return new StringReader(r.get(String.class));
    }
    
    public void patchResource(String uri) {
//	Client client = new Client();
//	WebResource r = client.resource("http://localhost:8080/xyz");
//	r.accept(MediaType.APPLICATION_XML_TYPE).header("", "");
//	r.setProperty(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true);
//	String response = r.method("PATCH", String.class);
//	System.out.println(response);
    }
    
    protected Object unmarshal(CAPresource cr) throws JAXBException {
	return unmarshal(cr.uri, cr.isfile);
    }
    
    protected Object unmarshal(String uri, boolean isfile) throws JAXBException {
	Object obj = unmarshaller.unmarshal(getResource(uri, isfile));
	if (CAPvec.hasEvents()) {
	    for (ValidationEvent ve: CAPvec.getEvents()) {
		System.err.println("*** Mashalling exception ***");
		System.err.println(ve.getMessage());
		ValidationEventLocator vel = ve.getLocator();
		System.err.println("Line number: "+vel.getLineNumber());
	    }
	    CAPvec.reset();
	}    
	return obj;
    }
    
    private void getDictionary() throws JAXBException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
	Dictionary CAPdict = (Dictionary)unmarshal(CAPresource.Dictionary);
	
	dict = new HashMap<String,Object>();

	Method[] methods = CAPdict.getClass().getDeclaredMethods();
	for (Method method: methods) {
	    if (method.getParameterTypes().length == 0 && method.getName().length() > 3 && method.getName().startsWith("get")) {
		//System.err.println(method.getName());
		Object obj = method.invoke(CAPdict);
		if (obj != null) {
		    Method innermethod = obj.getClass().getDeclaredMethods()[0];
		    Object obj2 = innermethod.invoke(obj);
		    if (innermethod.getReturnType().isAssignableFrom(List.class) && !innermethod.getName().startsWith("getElementH")) {
			String type1 = Character.toLowerCase(innermethod.getName().charAt(3))
				+innermethod.getName().substring(4);
			String type2 = innermethod.getName().substring(3);
			if (type2.equals("BiologyDescriptor")) type2 = "Element";
			if (type2.equals("AssayDescriptor")) type2 = "Element";
			if (type2.equals("InstanceDescriptor")) type2 = "Element";
			List<?> entries = (List<?>)obj2;
			for (Object entry: entries) {
			    String methodName = "get"+type2+"Id";
			    //System.err.println(methodName);
			    Method methodId = entry.getClass().getMethod(methodName, new Class[0]);
			    String key = methodId.invoke(entry).toString();
			    //System.err.println(type1+"/"+key);
			    dict.put(type1+"/"+key, entry);
			}
		    }
		}
	    }
	}
    }
    
    public Projects getProjects() throws JAXBException {
	return (Projects)unmarshal(CAPresource.Projects);
    }
    
    public Experiments getExperiments() throws JAXBException {
	return (Experiments)unmarshal(CAPresource.Experiments);
    }
    
    public Results getResults() throws JAXBException {
	return (Results)unmarshal(CAPresource.Results);
    }
    
    private Result getResult(String href) throws JAXBException {
	return (Result)unmarshal("resources/test/result.xml", true);
    }

    private Object link2Dict(Link link) {
	String href = link.getHref();
	if (href.contains("api/dictionary/")) {
	    String key = href.substring(href.indexOf("api/dictionary/")+15);
	    if (!(dict.containsKey(key)))
	    	System.err.println("!!!!"+key);
	    else
		return dict.get(key);
	}
	return link.getRel()+":"+href;
    }
    
    public void jaxbString(Object obj, Vector<String> levels) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
	Method[] methods = obj.getClass().getDeclaredMethods();
	for (Method method: methods) {
	    if (method.getParameterTypes().length == 0 && method.getName().length() > 3 && method.getName().startsWith("get")) {
		Object out = method.invoke(obj, new Object[0]);
		if (out instanceof ArrayList) {
		    ArrayList<?> al = (ArrayList<?>)out;
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
			int count = 1;
			levels.add(""+count);
			for (Iterator<?> it = ((ArrayList<?>)out).iterator(); it.hasNext(); ) {
			    Object item = it.next();
			    //System.out.println("LISTITEM");
			    if (item instanceof Link)
				item = link2Dict((Link)item);
			    if (item.getClass().toString().contains(".jaxb."))
				jaxbString(item, levels);
			    else 
				System.out.println(levels.toString()+"||"+item.getClass().toString()+" - "+item.toString());
			    count++;
			    levels.remove(levels.size()-1);
			    levels.add(""+count);
			}
			levels.remove(levels.size()-1);
		    } else {
			if (out instanceof Link)
			    out = link2Dict((Link)out);
			if (out.getClass().toString().contains(".jaxb.") && levels.size() < 20) {
			    if (levels.size() < 2 || !levels.get(levels.size()-1).equals(levels.get(levels.size()-2)))
				jaxbString(out, levels);
			} else 
			    System.out.println(levels.toString()+"|"+out.getClass().toString()+" - "+out.toString());
		    }
		    levels.remove(levels.size()-1);
		}
	    }
	}
	
	return;
    }
    
    public static void main(String[] args) {

	try {
	    CAPExtractor cape = new CAPExtractor();	    
	    
	    List<Project> ps = cape.getProjects().getProject();
	    List<?> es = cape.getExperiments().getExperimentAndLink();
	    List<Link> rs = cape.getResults().getLink();
	    for (Iterator<Link> it = rs.iterator(); it.hasNext();) {
		Link link = it.next();
		cape.jaxbString(link, new Vector<String>());
		if (link.getRel().equals("related") && link.getHref().contains("api/data/result/")) {
		    Result result = cape.getResult(link.getHref());
		    cape.jaxbString(result, new Vector<String>());
		    break;
		}
	    }
	    
	    
	    // Example roundtip / jaxb object clone
	    System.out.println(CAPUtil.jaxbHashMap(ps.get(0)));
	    Project projectClone = (Project)CAPUtil.jaxbConstructor(Project.class, CAPUtil.jaxbHashMap(ps.get(0)));
	    System.out.println(CAPUtil.jaxbHashMap(projectClone));
	    CAPUtil.jaxbString(projectClone, new Vector<String>());

	    cape.jaxbString(es.get(0), new Vector<String>());
	    
	} catch (Exception ex) {ex.printStackTrace();}
    }

}
