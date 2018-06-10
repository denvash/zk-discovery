package restful;

import model.util.ServiceInstance;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Path("/zk-srv-discovery")
@Produces({MediaType.TEXT_PLAIN})
public class Service {

    // Default ZK Path for discovery service.
    private static final String PATH = "/zk-srv-discovery";

    // Connecting to ZooKeeper from env-var
    private static final String ZK_CONNECT_STRING = System.getenv("zk_address");

    // Getting information from ZooKeeper
    private final Discovery discovery = new Discovery(PATH, ZK_CONNECT_STRING);

    /**
     * /zk-srv-discovery/health
     *
     * @return Connection status with zk.
     */
    @GET
    @Path("/health")
    @Produces({MediaType.TEXT_PLAIN})
    public Response getStatus() throws Exception {

        discovery.start();
        String entity = discovery.getStatus();
        Response response = Response.ok(entity).build();

        discovery.close();

        return response;
    }


    /**
     * /zk-srv-discovery/{serviceName}
     *
     * @return Data associated to supplied service name.
     */
    @GET
    @Path("/{ServiceName}")
    @Produces({MediaType.TEXT_PLAIN})
    public Response getServiceJson(@PathParam("ServiceName") final String serviceName) throws Exception {

        // Starts connection with zk.
        discovery.start();

        Object entity = discovery.getStatus().compareTo("IMOK") != 0 ?
                "NO CONNECTION" :
                toInstanceTable(discovery.queryForInstances(serviceName));

        Response response = Response.ok(entity).build();


        // Close connection with zk.
        discovery.close();

        return response;
    }

    /**
     * /zk-srv-discovery/getAll
     *
     * @return Displays all registered service's data available.
     */
    @GET
    @Path("/getAll")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response getAllServices(@DefaultValue(MediaType.TEXT_PLAIN)
                                   @QueryParam("MediaType") String mediaType) throws Exception {

        // Private case of getService
        discovery.start();

        Object entity = discovery.getStatus().compareTo("IMOK") != 0 ?
                "NO CONNECTION" :
                toTable(discovery.queryForInstances());

        Response response = Response.ok(entity).build();
        discovery.close();
        return response;
    }

    /**
     * Helping method for creating table
     *
     * @param collection - Collection of POJO service instances.
     */
    private String toInstanceTable(final Collection<ServiceInstance> collection) {

        StringBuilder table = new StringBuilder();
        for (ServiceInstance servicePOJO : collection) {
            table.append(servicePOJO.toTableRow()).append('\n');
        }

        return table.toString();
    }

    /**
     * Creates table of service instances registered to zk-service-discovery.
     *
     * @param collections - Collection of ServiceInstances.
     * @return Table as String.
     */
    private String toTable(final Collection<Collection<ServiceInstance>> collections) {

        StringBuilder table = new StringBuilder();

        table.append("Connected to ZK=")
                .append(ZK_CONNECT_STRING)
                .append('\n')
                .append("version=");

        table.append("zk-srv-discovery:")
                .append(System.getenv("SRV_VERSION"))
                .append('\n')
                .append('\n');

        // Building a header

        String s = "|";
        String space = " ";

        String date = String.format("%-22s", "Date");
        String serviceName = String.format("%-33s", "ServiceName");
        String host = String.format("%-51s", "Host: Name,IP,Port");
        String version = String.format("%-81s", "Version,MetaData,ZK-ID");

        table
                .append(s).append(space).append(date)
                .append(s).append(space).append(serviceName)
                .append(s).append(space).append(host)
                .append(s).append(space).append(version)
                .append(s)
                .append('\n');

        // Building separating line after header

        String u = "_";
        String first = StringUtils.leftPad(s, 24, u);
        String second = StringUtils.leftPad(s, 35, u);
        String third = StringUtils.leftPad(s, 53, u);
        String fourth = StringUtils.leftPad(s, 83, u);

        table
                .append(s).append(first)
                .append(second)
                .append(third)
                .append(fourth)
                .append('\n');

        collections.forEach(collection -> table.append(toInstanceTable(collection)));

        return table.toString();
    }
}
