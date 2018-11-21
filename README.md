# Description

**zk-registry** - A library for registering services to Zoo-Keeper
as Discovery Services.
After registering, you are able to check health status about the service -
by using `zk-srv-discovery` or with 
`org.apache.curator.x.discovery.ServiceDiscovery` instance.

# Requirements:
1. ZooKeeper 3.5.x
    * ZooKeeper 3.4.x - 
    need to make exclusion from curator's dependency:
    
            <dependency>
                <groupId>org.apache.curator</groupId>
                <artifactId>curator-x-discovery-server</artifactId>
                <version>4.0.1</version>
                <exclusions>
                    <exclusion>
                        <groupId>org.apache.zookeeper</groupId>
                        <artifactId>zookeeper</artifactId>
                    </exclusion>
                </exclusions>
            </dependency>
            
        and add zk dependency:
    
            <dependency>
                <groupId>org.apache.zookeeper</groupId>
                <artifactId>zookeeper</artifactId>
                <version>3.4.10</version>
            </dependency>

# Design
Implements static methods - register,unregister.
* Connecting to Zk server as Zk client.
* Zk client wrapped with `CuratorFramework`.
* Use the client for registering the service as Service-Discovery.
* ServiceDiscovery wrapped with `curator-x-discovery-server`

# Usage
1. Run ZooKeeper server.
2. Run `zk-srv-discovery` with ZooKeeper address
3. Register services using `zk-registry` library (or run `zk-registry-service`).
4. Use `zk-srv-discovery` web service to get information on registered services.

# API
The library contains a method which registering a service to the discovery service:
```java 
  public static int register(final String zkAddress,
                             final String serviceName,
                             final int servicePort,
                             final String version,
                             final String metaData)
 ```
 
 Examples: 
 * `register("123.54.23.1:2181", "Worker_1", 19005, "v1", "Mock Service 1")`
 * `register("123.54.23.1:2181", "Worker_2", 18006, "v3", "")`
 
 -------------------------------------------
 
 # zk-registry-service
A standalone service for registration to Discovery service.

# Requirements
1. `zk-registry` library
    
# Design
Depends on `zk-registry` library.

# Usage
Edit environment variables:

| **ENV_VAR** | *Example* | *Mandatory* |
| :------| :--------------| :----:|
| zk_address | zk_address=123.45.23.1:2181 | [x]
| service_name | service_name=Worker_1 | [x]
| service_port | service_port=18005 | [x]
| version | version=1.0.0 | [x]
| meta | meta="" | [ ]

Example of running an image:
```docker
docker run -d --name zk-register-Worker_1 \
-e zk_address=DENNIS-V:2181
-e service_name=WORKER_1 \
-e service_port=18006 \
-e version=1.0.3 \
-e meta="HELLO WORLD" \
zk-registry-service:1.0.0
```

# Docker Image
* To build a docker image run:

    `docker build --build-arg version=${version} -t ${image}`

    for example:
    
    `docker build --build-arg version=1.0.0 \
    -t zk-registry-service:1.0.0`

* Docker image build script:
    Run `./image-build.sh`
