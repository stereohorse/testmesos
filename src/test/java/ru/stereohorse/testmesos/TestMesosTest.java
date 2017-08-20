package ru.stereohorse.testmesos;

import com.spotify.docker.client.exceptions.ContainerNotFoundException;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static ru.stereohorse.testmesos.TestMesos.highAvailabilityMesosCluster;


public class TestMesosTest {

    private TestMesos mesos;


    @Before
    public void setupMesos() {
        mesos = highAvailabilityMesosCluster();
    }


    @Test
    @SneakyThrows
    public void shouldStartCluster() {

        String containerId = mesos.getZookeepers().firstContainerId();
        assertNotNull(mesos.getDocker().inspectContainer(containerId));

        try {
            mesos.getDocker().inspectContainer(containerId);
            fail(format("container %s not deleted", containerId));
        } catch (ContainerNotFoundException ignored) {
        }
    }


    @After
    public void destroyMesos() {
//        mesos.close();
    }
}