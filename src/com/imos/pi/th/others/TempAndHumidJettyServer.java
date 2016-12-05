package com.imos.pi.th.others;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pintu
 */
public class TempAndHumidJettyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempAndHumidJettyServer.class);

    private Server server;

    public void configure() {
        ResourceConfig config = new ResourceConfig();
        config.packages("com.imos.pi.th.rest");
        ServletHolder servlet = new ServletHolder(new ServletContainer(config));

        server = new Server(8085);
        ServletContextHandler context = new ServletContextHandler(server, "/*");
        context.addServlet(servlet, "/*");
        context.addFilter(TempHumidCORSFilter.class, "/*", null);
        LOGGER.info(TempHumidCORSFilter.class.getSimpleName() + " CORS filter is configured.");
    }

    public void start() {
        try {
            server.start();
            server.join();
            LOGGER.info("Jetty Embedded Server started.");
        } catch (Exception ex) {
            LOGGER.error("Jetty Embedded Server failed to start : " + ex.getMessage());
        } finally {
            server.destroy();
        }
    }
}
