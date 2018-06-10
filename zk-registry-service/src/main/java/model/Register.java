package model;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static model.Registry.register;
import static model.Registry.unregister;

/**
 * A model.model.Register Service for registry Discovery Service.
 * Uses environment variables to register to the Discovery server.
 * <p>
 * Mandatory env vars:
 * zk_address,service_name,service_address,service_port,version
 * Optional env vars:
 * meta_data
 * <p>
 * For Example: -Dzk_address=localhost:3181 -Dservice_name=Worker_1 -Dservice_address=localhost
 * -Dservice_port=18005 -Dversion=V1 -Dmeta_data="Mock Service From Maven"
 */
public class Register {
    public static void main(String... args) throws InterruptedException {
        String zooKeeperAddress = System.getenv("zk_address");
        String serviceName = System.getenv("service_name");
        String servicePort = System.getenv("service_port");
        String version = System.getenv("version");
        Optional<String> metaData = Optional.ofNullable(System.getenv("meta"));
        register(zooKeeperAddress, serviceName, Integer.parseInt(servicePort), version, metaData.orElse(""));

        CountDownLatch latch = new CountDownLatch(1);
        latch.await();

        unregister();
    }
}