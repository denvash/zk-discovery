package restful;

import model.util.ServiceInstance;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;

import java.io.Closeable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

/**
 * ZK Discovery service.
 * Connects to a valid ZK service,
 * Uses Curator-X-Discovery API to get information about registered services.
 * The main purpose of this class is to get information from zk
 * and convert Objects from zk to ServiceInstance
 */
class Discovery {
    private CuratorFramework client;
    private ServiceDiscovery<Object> serviceDiscovery;

    Discovery(final String path, final String zk_address) {
        // Connect as client to zk by using CuratorFramework.
        this.client = CuratorFrameworkFactory.newClient(zk_address, new RetryForever(5));

        // Start Service-Discovery on given path by using ServiceDiscovery in CuratorFramework
        this.serviceDiscovery = ServiceDiscoveryBuilder.builder(Object.class)
                .client(client)
                .basePath(path)
                .build();
    }

    String getStatus() throws Exception {

        final String STATUS_CONNECTED = "IMOK";
        final String STATUS_NOT_CONNECTED = "NO CONNECTION";

        client.getZookeeperClient().blockUntilConnectedOrTimedOut();

        return client.getZookeeperClient().isConnected() ? STATUS_CONNECTED : STATUS_NOT_CONNECTED;


    }

    void start() throws Exception {
        client.start();
        serviceDiscovery.start();
    }

    private static void closeAllQuietly(Closeable... closeable) {
        for (Closeable c : closeable) {
            CloseableUtils.closeQuietly(c);
        }
    }

    void close() {
        closeAllQuietly(client, serviceDiscovery);
    }

    /**
     * Collects all instances registered to given path.
     *
     * @return Collection of service instances as POJO.
     */
    Collection<Collection<ServiceInstance>> queryForInstances() throws Exception {
        Collection<Collection<ServiceInstance>> collection = new ArrayList<>();
        serviceDiscovery.queryForNames().forEach(serviceName -> {
            try {
                collection.add(queryForInstances(serviceName));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return collection;
    }

    /**
     * Collects all instances registered with given serviceName.
     * Converts services from zk (Objects) to readable ServiceInstance
     *
     * @return Collection of all Services under the same serviceName.
     */
    Collection<ServiceInstance> queryForInstances(final String serviceName) throws Exception {

        Collection<ServiceInstance> collection = new ArrayList<>();

        // For every ServiceInstance<Object>, convert it to ServiceInstance and add it to collection.
        // Parallel operation.
        serviceDiscovery.queryForInstances(serviceName).forEach(instance -> {

            LocalDateTime date =
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(instance.getRegistrationTimeUTC()), ZoneId.systemDefault());

            ServiceInstance pojo = new ServiceInstance(
                    instance.getName(),
                    instance.getAddress(),
                    instance.getPort(),
                    instance.getPayload().toString(),
                    date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    instance.getId()
            );
            collection.add(pojo);
        });
        return collection;
    }
}
