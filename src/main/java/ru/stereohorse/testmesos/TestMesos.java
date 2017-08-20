package ru.stereohorse.testmesos;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.NetworkConfig;
import com.spotify.docker.client.messages.NetworkCreation;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import ru.stereohorse.testmesos.components.ZookeeperCluster;
import ru.stereohorse.testmesos.containers.NamesGenerator.NamesBuilder;

import java.io.Closeable;

import static ru.stereohorse.testmesos.containers.NamesGenerator.namesBuilder;

@Builder
@Getter
public class TestMesos implements TestRule, Closeable {

    private final DockerClient docker;
    private final NetworkCreation network;
    private final ZookeeperCluster zookeepers;


    @SneakyThrows
    @SuppressWarnings("WeakerAccess")
    public static TestMesos highAvailabilityMesosCluster() {

        final DefaultDockerClient docker = DefaultDockerClient
                .fromEnv()
                .build();

        final NamesBuilder namesBuilder = namesBuilder();

        final NetworkCreation network = docker.createNetwork(
                NetworkConfig.builder()
                        .name(namesBuilder.getTestId())
                        .driver("bridge")
                        .attachable(true)
                        .build());

        return TestMesos.builder()
                .docker(docker)
                .network(network)
                .zookeepers(ZookeeperCluster.builder()
                        .docker(docker)
                        .network(network)
                        .containerNamesGenerator(namesBuilder
                                .containerNameGeneratorWithPrefix("zoo"))
                        .build()
                        .start())
                .build();
    }

    @Override
    public Statement apply(Statement statement, Description d) {

        try {

            // throwing an exception from the TestRule will
            // result in undefined behavior
            // http://junit.org/junit4/javadoc/4.12/org/junit/ClassRule.html

            statement.evaluate();

        } catch (Throwable t) {
            t.printStackTrace(System.err);
        } finally {
            close();
        }

        return statement;
    }

    public void close() {
        ignoringErrors(zookeepers::close);
        ignoringErrors(() -> docker.removeNetwork(network.id()));
        ignoringErrors(docker::close);
    }

    private void ignoringErrors(CheckedExecution execution) {
        try {
            execution.run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    @FunctionalInterface
    public interface CheckedExecution {
        void run() throws Exception;
    }
}
