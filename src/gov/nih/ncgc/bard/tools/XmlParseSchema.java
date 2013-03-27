// $Id: XmlPipe.java 3212 2009-09-04 20:24:24Z nguyenda $

package gov.nih.ncgc.bard.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlParseSchema extends PipedInputStream {
    static final Logger logger = Logger.getLogger(XmlParseSchema.class.getName());

    PrintWriter out;
    String stopElement = null;

    public XmlParseSchema () throws Exception {
	out = new PrintWriter (new PipedOutputStream (this), false);
    }

    public void parse (InputStream is) throws Exception {
	SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
	parser.parse(is, new XmlHandler ());
	out.close();
    }

    public void setStopElement (String stopElement) { 
	this.stopElement = stopElement; 
    }
    public String getStopElement () { return stopElement; }

    class XmlHandler extends DefaultHandler {
	Stack<String> stack = new Stack<String>();
	HashMap<String,Vector<String>> tree = new HashMap<String,Vector<String>>();
	boolean done = false;

	@Override
	public void characters (char[] ch, int start, int length) {
	    if (!done) {
//		out.write(ch, start, length);
	    }
	}
	
	public void endElement (String uri, String local, String qname) {
	    logger.info("uri="+uri+" local="+local+" qname="+qname);
	    if (!done) {
//		out.print("</"+(uri.length() >0?(uri+":"):"")+qname+">");
		String pname = stack.pop(); // pname = qname
	    }
	}

	@Override
	public void notationDecl (String name, String publicId, String sysId) {
	    logger.info("name="+name+" public="+publicId+" system="+sysId);
	}

	@Override
	public void processingInstruction (String target, String data) {
	    logger.info("target="+target+" data="+data);
	}

	@Override
	public void startDocument () {
//	    out.println("<?xml version=\"1.0\"?>");
	    done = false;
	}

	@Override
	public void endDocument () {
//	    out.flush();
	    Vector<String> key = new Vector<String>();
	    key.add(null); // top of the tree
	    Vector<Integer> index = new Vector<Integer>();
	    index.add(0);
	    while (index.size() > 0) {
//		System.out.println(tree);
//		System.out.println(key);
//		System.out.println(index);
		if (index.lastElement() > tree.get(key.lastElement()).size()-1) {
		    key.remove(key.size()-1);
		    index.remove(index.size()-1);
		}
		else {
		    String inset = "";
		    for (int i=1; i<index.size(); i++) inset = inset + "\t";
		    String item = tree.get(key.lastElement()).get(index.lastElement());
		    System.out.println(inset+item);
		    if (tree.containsKey(item) && !item.equals(key.lastElement())) {key.add(item); index.add(-1);}
		}
		if (index.size() > 0) index.set(index.size()-1, index.lastElement() + 1);
	    }
	}

	@Override
	public void startElement (String uri, String local, 
				  String qname, Attributes attrs) {
	    if (done) return;

	    if (stopElement != null) {
		if (qname.equals(stopElement)) {
		    while (!stack.isEmpty()) {
			qname = stack.pop();
//			out.println("</"+qname+">");
		    }
		    done = true;
		    return;
		}
	    }

	    StringBuilder sb = new StringBuilder ();
	    sb.append("<"+(uri.length()>0?(uri+":"):"")+qname);
	    for (int i = 0; i < attrs.getLength(); ++i) {
		sb.append(" "+attrs.getQName(i)+"=\""+attrs.getValue(i)+"\"");
	    }
	    sb.append(">");
	    String key = null;
	    if (!stack.empty()) key = stack.peek();
	    if (!tree.containsKey(key)) tree.put(key, new Vector<String>());
	    if (!tree.get(key).contains(qname))
		tree.get(key).add(qname);
	    
	    stack.push(qname);
//	    out.print(sb.toString());
	    logger.info("uri="+uri+" local="+local+" qname="+qname);
	}

	@Override
	public void unparsedEntityDecl (String name, String publicId, 
					String sysId, String notation) {
	    logger.info("name="+name+" publicId="+publicId+" systemId="+sysId
			+" notation="+notation);
	}

	@Override
	public void error (SAXParseException ex) {
	    logger.log(Level.SEVERE, "SAX Exception", ex);
	}
    }

    public static void main (String[] argv) throws Exception {

	final XmlParseSchema xml = new XmlParseSchema ();
//	xml.setStopElement("PC-AssaySubmit_data");
	xml.parse(new FileInputStream(argv[0]));
	
	
//	final URL url = new URL
//	    ("ftp://ftp.ncbi.nlm.nih.gov/pubchem/Bioassay/XML/1708.xml.gz");
//	final ExecutorService service = Executors.newSingleThreadExecutor();
//	service.execute(new Runnable () {
//		public void run () {
//		    try {
//			xml.parse(new GZIPInputStream (url.openStream()));
//		    }
//		    catch (Exception ex) {
//			ex.printStackTrace();
//		    }
//		}
//	    });

	Reader reader = new InputStreamReader (xml);
	try {
	    char[] buf = new char[1024];
	    for (int nc; (nc = reader.read
			  (buf, 0, buf.length)) > 0; ) {
		System.out.print(new String (buf, 0, nc));
	    }
	}
	catch (IOException ex) {
	    //ex.printStackTrace();
	}

//	service.shutdown();
    }
}
