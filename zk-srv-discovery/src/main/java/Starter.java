import restful.Service;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

import static model.Registry.register;

/**
 * A Service Discovery Starter, the service starts on
 * http://localhost:8080/v1/application.wadl
 * Starts Http server using grizzly API.
 * The server is RESTful using jersey API
 */
public class Starter implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Starter.class);

    private static final String ResourcePackage = "control.rest";
    private static final CountDownLatch latch = new CountDownLatch(1);

    private static final String zooKeeperAddress = System.getenv("zk_address");
    private static final String serviceName = "zk-discovery-service";
    private static final int servicePort = 8080;
    private static final String version = "1.0.1";
    private static final String metaData = "";

    private static void startServer() throws UnknownHostException {

        // Set service address from local-host
        InetAddress inetAddress = InetAddress.getLocalHost();
        String BASE_URI = "http://" + inetAddress.getHostAddress() + ":8080/v1/";

        // Build resources for RESTful web service and start Http Server as REST.
        final ResourceConfig rc = new ResourceConfig().registerClasses(Service.class);
        GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);

        register(zooKeeperAddress, serviceName, servicePort, version, metaData);

        logger.info("server started at " + BASE_URI + "application.wadl");
    }

    public static void main(String... args) {
        logger.info("Running zk-srv-discovery:" + System.getenv("SRV_VERSION"));

        try {
            startServer();
            latch.await();
        } catch (InterruptedException | UnknownHostException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        main();
    }
}
