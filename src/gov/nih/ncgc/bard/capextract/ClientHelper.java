package gov.nih.ncgc.bard.capextract;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.MessageProperties;

/**
 * Handle HTTPS connections.
 * <p/>
 * Taken from https://gist.github.com/1069465
 *
 * @author Rajarshi Guha
 */
public class ClientHelper {
    private static Configuration configureClient() {
        TrustManager[] certs = new TrustManager[]{
                new X509TrustManager() {

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }


                    public void checkServerTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }

                    public void checkClientTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }
                }
        };
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, certs, new SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
        }
        assert ctx != null;
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

        
        
        
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
        	return true;
            }
        };

        // 'New' jersey...
        ClientBuilder builder = ClientBuilder.newBuilder().sslContext(ctx);
        Client client = builder.hostnameVerifier(hostnameVerifier).build();
        Configuration config = client.getConfiguration();
        
        // Old Jersey...
        //ClientConfig config = new ClientConfig();
//        try {
//            config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
//                    new HostnameVerifier() {
//                        public boolean verify(String hostname, SSLSession session) {
//                            return true;
//                        }
//                    },
//                    ctx
//            ));
//        } catch (Exception e) {
//        }

        // should be getting a better SAX parser, but there's only going to be
        // a single source of XML documents
     
        // 'New' jersey :) 
        config.getProperties().put(MessageProperties.XML_SECURITY_DISABLE, Boolean.TRUE);
        // old jersey
        // config.getFeatures().put(FeaturesAndProperties.FEATURE_DISABLE_XML_SECURITY, true);
        
        return config;
    }

    public static Client createClient() {
	// new jersey
	return ClientBuilder.newBuilder().build();
        // old jersey
	//return Client.create(ClientHelper.configureClient());
    }
}
