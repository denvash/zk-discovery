import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static model.Registry.register;
import static model.Registry.unregister;
import static org.junit.Assert.*;

public class RegistryTest {

    private static final Logger logger = LoggerFactory.getLogger(RegistryTest.class);
    private static final String PATH = "/zk-srv-discovery";
    private static final int SUCCESS = 0;
    private static final int N = 10;
    private static TestingServer ts1;
    private static TestingServer ts2;
    private static MockService mockService;

    static {
        try {
            ts1 = new TestingServer();
            ts2 = new TestingServer();
        } catch (Exception e) {
            logger.error("Initialing Testing Server Error", e);
        }
    }

    @AfterClass
    public static void tearDown() throws IOException {
        unregister(); //Only current thread
        ts1.close();
        ts2.close();
    }

    @Before
    public void setUp() throws Exception {
        ts1.start();
        ts2.start();

        mockService = new MockService("Test", 1800, "1.0.0");
    }

    @Test
    public void testRegisterNMockServices() throws Exception {

        List<String> servicesNames = new ArrayList<>();

        for (int i = 0; i < N; ++i) {
            assertEquals(register(ts1.getConnectString(),
                    mockService.getName() + i,
                    mockService.getPort() + i,
                    mockService.getVersion() + i,
                    Integer.toString(i)), SUCCESS);
            servicesNames.add(mockService.getName() + i);
        }

        //connect to zk
        CuratorFramework client = CuratorFrameworkFactory.newClient(
                ts1.getConnectString(),
                new ExponentialBackoffRetry(1000, 3)
        );

        client.start();

        assertNotNull(client.checkExists().forPath(PATH)); //returns null if path not created.

        //Get registered services.
        ServiceDiscovery<Object> serviceDiscovery = ServiceDiscoveryBuilder.builder(Object.class)
                .client(client)
                .basePath(PATH)
                .build();

        Collection<String> fromZK = serviceDiscovery.queryForNames();

        Collection<ServiceInstance<Object>> serviceInstances = serviceDiscovery.queryForInstances("Test0");

        assertFalse(serviceInstances.isEmpty());

        assertTrue(fromZK.containsAll(servicesNames));

    }

    @Test
    public void testRegisterToMultiZk() {

        assertEquals(register(ts1.getConnectString(),
                mockService.getName(),
                mockService.getPort(),
                mockService.getVersion(),
                "Hello"), SUCCESS);

        assertEquals(register(ts2.getConnectString(),
                mockService.getName(),
                mockService.getPort(),
                mockService.getVersion(),
                "Hello"), SUCCESS);
    }

    private class MockService {
        private String name;
        private int port;
        private String version;

        MockService(final String name, final int port, final String version) {

            this.name = name;
            this.port = port;
            this.version = version;
        }

        String getName() {
            return name;
        }

        int getPort() {
            return port;
        }

        String getVersion() {
            return version;
        }
    }

}