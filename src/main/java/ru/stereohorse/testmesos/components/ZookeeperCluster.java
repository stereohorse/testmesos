package ru.stereohorse.testmesos.components;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.NetworkCreation;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import ru.stereohorse.testmesos.containers.Container;
import ru.stereohorse.testmesos.containers.NamesGenerator;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.spotify.docker.client.DockerClient.RemoveContainerParam.forceKill;
import static com.spotify.docker.client.DockerClient.RemoveContainerParam.removeVolumes;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;


@Builder
@Getter(PRIVATE)
public class ZookeeperCluster implements Closeable {

    private final DockerClient docker;
    private final NetworkCreation network;
    private final NamesGenerator containerNamesGenerator;

    private List<ContainerCreation> zoos;


    @Override
    public void close() throws IOException {
        zoos.forEach(zoo -> {
            try {
                docker.removeContainer(
                        zoo.id(),
                        forceKill(),
                        removeVolumes());

            } catch (DockerException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public ZookeeperCluster start() {

        final List<String> containerNames = Stream
                .generate(containerNamesGenerator::next)
                .limit(3)
                .collect(toList());

        zoos = IntStream.range(0, containerNames.size())
                .mapToObj(i -> Container.builder()
                        .name(containerNames.get(i))
                        .config(ContainerConfig.builder()
                                .image("zookeeper:3.4.10")
                                .hostname(containerNames.get(i))
                                .env(asList(
                                        "ZOO_MY_ID=" + (i + 1),
                                        zkServersFrom(containerNames)
                                ))
                                .build())
                        .build())
                .map(this::createContainer)
                .map(this::startContainer)
                .map(container -> attach(container, network))
                .collect(toList());

        return this;
    }


    public String firstContainerId() {
        return zoos.get(0)
                .id();
    }


    @SneakyThrows
    private ContainerCreation createContainer(Container container) {
        return docker.createContainer(
                container.getConfig(),
                container.getName());
    }

    @SneakyThrows
    private ContainerCreation startContainer(ContainerCreation container) {
        docker.startContainer(container.id());
        return container;
    }

    @SneakyThrows
    private ContainerCreation attach(
            ContainerCreation container,
            NetworkCreation network) {

        docker.connectToNetwork(container.id(), network.id());
        return container;
    }

    private String zkServersFrom(List<String> hosts) {

        return "ZOO_SERVERS=" + IntStream.range(0, hosts.size())
                .mapToObj(i -> format(
                        "server.%s=%s:2888:3888",
                        i + 1,
                        hosts.get(i)))
                .collect(joining(" "));
    }
}
