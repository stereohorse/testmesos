package ru.stereohorse.testmesos.containers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;

public interface NamesGenerator extends Iterator<String> {

    @Override
    default boolean hasNext() {
        return true;
    }

    static NamesBuilder namesBuilder() {
        return new NamesBuilder();
    }

    @RequiredArgsConstructor
    class ContainerNameGenerator implements NamesGenerator {

        private final AtomicInteger idsCounter = new AtomicInteger();

        private final String testId;
        private final String prefix;


        @Override
        public String next() {
            return format("%s-%s-%s",
                    testId,
                    prefix,
                    idsCounter.incrementAndGet());
        }
    }


    class NamesBuilder {

        @Getter
        private final String testId = format(
                "mesostest-%s",
                randomUUID()
                        .toString()
                        .replaceAll("-", ""));

        public NamesGenerator containerNameGeneratorWithPrefix(
                String prefix) {

            return new ContainerNameGenerator(testId, prefix);
        }
    }
}
