import org.apache.commons.lang3.StringUtils;
import org.apache.curator.test.TestingServer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static model.Registry.register;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiscoveryTest {

    public class MockService {
        private String name;
        private int port;
        private String version;

        MockService(String name, int port, String version) {
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

    private static final int SUCCESS = 0;
    private static final int SERVICE_NAME_INDEX = 4;
    private static final int HEALTH_INDEX = 0;

    private static final String ZK_ENV_VAR = "zk_address";
    private static final String HEALTH_MSG = "IMOK";
    private static final String MOCK_SERVICE_NAME = "Worker_1";
    private static final String GET_ALL = "getAll";
    private static final int N = 10;

    // As return from getAll verb request

    private static final int START_INDEX_OF_WORKER = 26;
    private static final int END_INDEX_OF_WORKER = 35;

    @ClassRule
    public static EnvironmentVariables environmentVariables = new EnvironmentVariables();
    private static String url;
    private static URL obj;
    private static HttpURLConnection openConnection;
    private static TestingServer ts;
    private static BufferedReader inputStream;
    private static InetAddress inetAddress;
    private MockService worker = new MockService(MOCK_SERVICE_NAME, 18005, "1.0.0");

    @BeforeClass
    public static void setUp() throws Exception {

        Logger.getRootLogger().setLevel(Level.OFF);

        inetAddress = InetAddress.getLocalHost();
        ts = new TestingServer();
        ts.start();

        // set env var zk_address to Curator-Testing ZK address
        environmentVariables.set(ZK_ENV_VAR, ts.getConnectString());
        Assert.assertThat(System.getenv(ZK_ENV_VAR), is(ts.getConnectString()));


        // run discovery service
        final Thread discoveryThread = new Thread(new Starter());
        discoveryThread.setName("Discovery Thread");
        discoveryThread.start();
        Thread.sleep(2000); // Spare time for discovery service to start;
    }

    @AfterClass
    public static void tearDown() {
        try {
            ts.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testDiscoveryAfterZKRestart() throws Exception {

        testGetServiceVerb();

        // Restart zk and analyze output again.
        ts.restart();

        // spare time
        Thread.sleep(2000);

        // connection is closed, reopen;
        openConnection = buildConnection(obj);

        // make get request and analyze output.
        inputStream = getInputStream(openConnection);
        assertResponse(worker.getName(), inputStream, SERVICE_NAME_INDEX, " ");
    }

    @Test
    public void testGetAllServiceVerb() throws Exception {
        assertTrue(RESTfulServiceIsUp());

        List<String> workers = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            assertEquals(register(ts.getConnectString(),
                    worker.getName() + i,
                    worker.getPort() + i,
                    worker.getVersion() + i,
                    "Check" + i
            ), SUCCESS);
            workers.add(worker.getName() + i);
        }

        // make GET request for all services.
        url = "http://" + inetAddress.getHostAddress() + ":8080/v1/zk-srv-discovery/" + GET_ALL;

        obj = new URL(url);
        openConnection = buildConnection(obj);

        // make GET request and analyze output.
        inputStream = getInputStream(openConnection);

        List<String> registeredServices = new ArrayList<>();

        inputStream.lines().forEach(x -> {
            if (x.contains(MOCK_SERVICE_NAME)) {
                registeredServices.add(x.substring(START_INDEX_OF_WORKER, END_INDEX_OF_WORKER));
            }
        });

        // OVERLOADED test service with Worker_1, so it's 2N services.
        assertEquals(registeredServices.size(), N * 2);
        assertTrue(registeredServices.containsAll(workers));
        inputStream.close();
    }

    @Test
    public void testHealthVerb() throws Exception {
        assertTrue(RESTfulServiceIsUp());
        url = "http://" + inetAddress.getHostAddress() + ":8080/v1/zk-srv-discovery/health";

        obj = new URL(url);
        openConnection = buildConnection(obj);
        inputStream = getInputStream(openConnection);
        assertResponse(HEALTH_MSG, inputStream, HEALTH_INDEX, "");
    }

    @Test
    public void testGetServiceVerb() throws Exception {
        assertTrue(RESTfulServiceIsUp());

        for (int i = 0; i < N; i++) {
            assertEquals(register(ts.getConnectString(),
                    worker.getName(),
                    worker.getPort(),
                    worker.getVersion(),
                    "Check")
                    , SUCCESS
            );
        }

        // make GET request for a single service.
        url = "http://" + inetAddress.getHostAddress() + ":8080/v1/zk-srv-discovery/" + MOCK_SERVICE_NAME;

        obj = new URL(url);
        openConnection = buildConnection(obj);

        // make GET request and analyze output.
        inputStream = getInputStream(openConnection);
        assertResponse(worker.getName(), inputStream, SERVICE_NAME_INDEX, " ");
        inputStream.close();
    }



    private BufferedReader getInputStream(@NotNull final HttpURLConnection openConnection) throws IOException {
        return new BufferedReader(new InputStreamReader(openConnection.getInputStream()));
    }

    private HttpURLConnection buildConnection(@NotNull final URL obj) throws IOException {
        HttpURLConnection openConnection = (HttpURLConnection) obj.openConnection();
        openConnection.setRequestMethod("GET");
        assertEquals("OK", openConnection.getResponseMessage());
        return openConnection;
    }

    private void assertResponse(final String serviceName, @NotNull final BufferedReader in,
                                final int responseIndex,
                                final String separator) throws IOException {
        String[] response = StringUtils.split(in.readLine(), separator);
        Assert.assertThat(serviceName, is(response[responseIndex]));
    }

    private boolean RESTfulServiceIsUp() throws Exception {
        String url = "http://" + inetAddress.getHostAddress() + ":8080/v1/application.wadl";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        return con.getResponseMessage().equals("OK");
    }
}