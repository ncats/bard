package gov.nih.ncgc.bard.tools;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;

import java.io.File;
import java.io.FileInputStream;


/**
 * Runs the API standalong using an embedded Jetty container.
 * <p/>
 * This is useful for debugging and if desired a single bundled
 * application. Currently the project does not include the Jetty
 * dependencies, so you'll have to get them separately and add
 * them to your class path. This has been tested with Jetty 7.
 * <p/>
 * In addition, to support JNDI resources for pooled database connections
 * you'll need the following libraries in your CLASSPATH as well
 * <ul>
 * <li>commons-dbcp-1.4.jar</li>
 * <li>commons-pool-1.6.jar</li>
 * <li>mysql-connector-java-5.1.19-bin.jar</li>
 * </ul>
 * Finally, you'll need to edit your jetty.xml file to include something
 * like
 * <code>
 * <New id="mysqlpds" class="org.eclipse.jetty.plus.jndi.Resource">
 * <Arg>jdbc/myidentifier</Arg>
 * <Arg>
 * <New class="org.apache.commons.dbcp.BasicDataSource">
 * <Set name="driverClassName">com.mysql.jdbc.Driver</Set>
 * <Set name="Url">jdbc:mysql://host:port/dbname?autoReconnect=true</Set>
 * <Set name="Username">your_username</Set>
 * <Set name="Password">yourpassword</Set>
 * </New>
 * </Arg>
 * </New>
 * <p/>
 * </code>
 * where <code>jdbc/myidentifier</code> is the String you use when
 * lookup the JNDI resource.
 *
 * @author Rajarshi Guha
 */
public class JettyRunner {

    /**
     * Start embedded Jetty server.
     *
     * @param args Command line arguments. Currently you just need to specify the path to jetty.xml
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Must specify path to jetty.xml");
            System.exit(-1);
        }
        File configFile = new File(args[0]);
        XmlConfiguration configuration = new XmlConfiguration(new FileInputStream(configFile));
        Server server = (Server) configuration.configure();
        Connector connector = new SelectChannelConnector();
        connector.setPort(8080);
        connector.setHost("127.0.0.1");
        server.addConnector(connector); 
        WebAppContext wac = new WebAppContext();
        wac.setContextPath("/");
        wac.setDescriptor("web/WEB-INF/web.xml");
        wac.setResourceBase("classes");
        wac.setParentLoaderPriority(true);
        server.setHandler(wac);
        server.setStopAtShutdown(true);
        server.start();
        server.join();
    }
}
