package gr.ntua.ece.cslab.e2datascheduler.server;

import gr.ntua.ece.cslab.e2datascheduler.ws.CORSFilter;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.ResourceBundle;


public class HaierServer {

    public static ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static final int HAIER_PORT = Integer.parseInt(resourceBundle.getString("haier.port"));

    private static Server configureServer() {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("gr.ntua.ece.cslab.e2datascheduler.ws");
        resourceConfig.register(MultiPartFeature.class);
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new CORSFilter());
        ServletContainer servletContainer = new ServletContainer(resourceConfig);
        ServletHolder sh = new ServletHolder(servletContainer);
        Server server = new Server(HAIER_PORT);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        server.setHandler(context);
        return server;
    }

    public static void main(String[] args){
        Server server = configureServer();
        try {
            server.start();
            server.join();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            server.destroy();
        }
    }

}
