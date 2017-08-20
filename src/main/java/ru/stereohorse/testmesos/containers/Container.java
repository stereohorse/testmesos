package ru.stereohorse.testmesos.containers;


import com.spotify.docker.client.messages.ContainerConfig;
import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
public class Container {

    private final String name;
    private final ContainerConfig config;
}
