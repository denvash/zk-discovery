package model.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Simple Service Instance POJO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ServiceInstance {

    @JsonProperty("name")
    private final String serviceName;
    @JsonProperty("address")
    private final String address;
    @JsonProperty("port")
    private final int port;
    @JsonProperty("payload")
    private final String payload;
    @JsonProperty("date")
    private final String date;
    @JsonProperty("id")
    private final String id;

    public ServiceInstance(final String serviceName,
                           final String address,
                           final int port,
                           final String payload,
                           final String date,
                           final String id) {
        this.serviceName = serviceName;
        this.address = address;
        this.port = port;
        this.payload = payload;
        this.date = date;
        this.id = id;
    }

    @Override
    public String toString() {
        return "ServiceInstance{" +
                "serviceName='" + serviceName + '\'' +
                ", address='" + address + '\'' +
                ", port=" + port +
                ", payload=" + payload +
                ", date='" + date + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    public String toTableRow() {
        TableBuilder tb = new TableBuilder();
        final String S = "|";

        String dateCol = String.format("%-21s",date);
        String serviceCol = String.format("%-32s",serviceName);

        String hostInfo = address + " " + Integer.toString(port);
        String hostCol = String.format("%-50s", hostInfo);

        String version = payload + " " + "[" + id + "]";
        String versionCol = String.format("%-80s", version);


        tb.addRow(
                S, dateCol,
                S, serviceCol,
                S, hostCol,
                S, version, S
        );

        return tb.toString();
    }
}
