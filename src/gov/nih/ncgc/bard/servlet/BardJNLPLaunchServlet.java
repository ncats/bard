package gov.nih.ncgc.bard.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BardJNLPLaunchServlet extends HttpServlet {

	public void service(HttpServletRequest req,
			HttpServletResponse res) throws IOException {
		ResourceBundle resource = ResourceBundle.getBundle("gov.nih.ncgc.bard.props.bard");
		String restURL = resource.getString("local.rest.base.url");
		String codeBaseURL = resource.getString("local.bard.client.jnlp.codebase");

		//grab the entity argument
		String entityArg = req.getParameter("entity");
		if(entityArg != null)
			entityArg = "<argument>"+entityArg+"</argument>";
		else
			entityArg = "";

		res.setContentType("application/x-java-jnlp-file");

		PrintWriter out = res.getWriter();  		
		String jnlp = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
				"<jnlp spec=\"1.0+\" codebase=\""+codeBaseURL+"\" href=\"\">"+
				"<information>"+
				"<title>Bard Client</title>"+
				"<vendor>BARD</vendor>"+
				"<offline-allowed/>"+
				"</information>"+
				"<resources>"+
				"<j2se version=\"1.6+\" href=\"http://java.sun.com/products/autodl/j2se\"" +
				" initial-heap-size=\"256m\" max-heap-size=\"1024m\"/>"+
				"<jar href=\"bard-client.jar\" main=\"true\" />"+
				"<jar href=\"AppFramework.jar\"/>"+
				"<jar href=\"asm-4.0.jar\"/>"+
				"<jar href=\"BrowserLauncher2-12.jar\"/>"+
				"<jar href=\"commons-dbcp-1.3.jar\"/>"+
				"<jar href=\"commons-logging-1.1.1.jar\"/>"+
				"<jar href=\"commons-pool-1.5.5.jar\"/>"+
				"<jar href=\"datanucleus-api-jdo-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-api-jpa-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-api-rest-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-awtgeom-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-cache-3.1.1.jar\"/>"+
				"<jar href=\"datanucleus-core-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-enhancer-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-excel-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-googlecollections-3.0.0-release.jar\"/>"+
				"<jar href=\"datanucleus-hbase-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-jdo-query-3.0.2.jar\"/>"+
				"<jar href=\"datanucleus-jodatime-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-jpa-query-3.0.2.jar\"/>"+
				"<jar href=\"datanucleus-json-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-ldap-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-mongodb-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-neodatis-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-odf-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-rdbms-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-spatial-3.1.0-release.jar\"/>"+
				"<jar href=\"datanucleus-xml-3.1.0-release.jar\"/>"+
				"<jar href=\"ehcache-core-2.2.0.jar\"/>"+
				"<jar href=\"eventbus-1.4.jar\"/>"+
				"<jar href=\"forms-1.1.0.jar\"/>"+
				"<jar href=\"geronimo-jpa_2.0_spec-1.1.jar\"/>"+
				"<jar href=\"geronimo-jta_1.1_spec-1.1.jar\"/>"+
				"<jar href=\"glazedlists.jar\"/>"+
				"<jar href=\"google-collections-1.0.jar\"/>"+
				"<jar href=\"h2-1.3.167.jar\"/>"+
				"<jar href=\"hadoop-core-1.0.0.jar\"/>"+
				"<jar href=\"hbase-0.90.4.jar\"/>"+
				"<jar href=\"hsqldb-1.8.0.4.jar\"/>"+
				"<jar href=\"httpclient-4.1.1.jar\"/>"+
				"<jar href=\"httpcore-4.1.jar\"/>"+
				"<jar href=\"httpmime-4.1.1.jar\"/>"+
				"<jar href=\"jackson-core-asl-1.9.2.jar\"/>"+
				"<jar href=\"jackson-mapper-asl-1.9.2.jar\"/>"+
				"<jar href=\"jaxb-api-2.1.jar\"/>"+
				"<jar href=\"jaxb-impl-2.1.jar\"/>"+
				"<jar href=\"jchem.jar\"/>"+
				"<jar href=\"jcommon-1.0.17.jar\"/>"+
				"<jar href=\"jdo-api-3.1-SNAPSHOT-20110926.jar\"/>"+
				"<jar href=\"jep-2.4.1.jar\"/>"+
				"<jar href=\"jfreechart-1.0.14.jar\"/>"+
				"<jar href=\"jide-common.jar\"/>"+
				"<jar href=\"joda-time-1.6.jar\"/>"+
				"<jar href=\"jxlayer.jar\"/>"+
				"<jar href=\"log4j-1.2.14.jar\"/>"+
				"<jar href=\"looks-2.1.4.jar\"/>"+
				"<jar href=\"miglayout-3.7.2-swing.jar\"/>"+
				"<jar href=\"mongo-java-driver-2.5.2.jar\"/>"+
				"<jar href=\"mysql-connector-java-5.1.20-bin.jar\"/>"+
				"<jar href=\"neodatis-odb-1.9.30.689.jar\"/>"+
				"<jar href=\"odfdom-java-0.8.7.jar\"/>"+
				"<jar href=\"poi-3.6.jar\"/>"+
				"<jar href=\"poi-ooxml-3.6.jar\"/>"+
				"<jar href=\"slf4j-api-1.5.6.jar\"/>"+
				"<jar href=\"slf4j-log4j12-1.5.6.jar\"/>"+
				"<jar href=\"swingworker-0.8.0.jar\"/>"+
				"<jar href=\"swingx.jar\"/>"+
				"<jar href=\"time-api-0.6.3.jar\"/>"+
				"<jar href=\"xercesImpl-2.8.1.jar\"/>"+
				"</resources>"+
				"<application-desc name=\"Bard Client\" main-class=\"bard.ui.main.BARD\" " +
				"width=\"300\" height=\"300\">"+
				"<argument>"+restURL+"</argument>"
				+entityArg+  //optional entity argument
				"</application-desc>"+
				"<update check=\"background\"/>"+
				"</jnlp>";
		out.println(jnlp);
	}

}

