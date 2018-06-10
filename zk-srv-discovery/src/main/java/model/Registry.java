package model;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.apache.log4j.Logger;

import java.net.InetAddress;

public class Registry {

    private static final String PATH = "/zk-srv-discovery";
    private static final int SUCCESS = 0;
    private static final int FAIL = 1;

    private static CuratorFramework client;
    private static ServiceDiscovery<Object> serviceDiscovery;
    private static Logger logger = Logger.getLogger(Registry.class);


    /**
     * Registers a service to ZK.
     * Registers a service per Thread.
     * <p>
     * Example:
     * register("localhost:3181", "Worker_1", 18005, "V1", "Mock server 1");
     *
     * @return SUCCESS or FAIL;
     */
    public static int register(final String zkAddress,
                               final String serviceName,
                               final int servicePort,
                               final String version,
                               final String metaData) {

        //Connecting to ZooKeeper as client.
        client = CuratorFrameworkFactory.newClient(zkAddress,
                new ExponentialBackoffRetry(1000, 3));
        client.start();

        //Creates Service Instance according to params.
        try {
            ServiceInstance<Object> serviceInstance = ServiceInstance.builder()
                    .name(serviceName)
                    .address(InetAddress.getLocalHost().getHostName() + " " + InetAddress.getLocalHost().getHostAddress())
                    .port(servicePort)
                    .payload("[" + version + "]" + " " + "[" + metaData + "]")
                    .uriSpec(new UriSpec("{scheme}://{address}:{servicePort}"))
                    .build();

            serviceDiscovery = ServiceDiscoveryBuilder.builder(Object.class)
                    .basePath(PATH)
                    .client(client)
                    .thisInstance(serviceInstance)
                    .build();

            //Connects to ZooKeeper as Service Instance.
            serviceDiscovery.start();

            logger.info("Client:Connected, ServiceDiscovery: Connected");

        } catch (Exception e) {
            logger.error("Failed connecting to ZooKeeper", e);
            return FAIL;
        }
        return SUCCESS;
    }

    public static void unregister() {
        CloseableUtils.closeQuietly(serviceDiscovery);
        CloseableUtils.closeQuietly(client);
    }
}