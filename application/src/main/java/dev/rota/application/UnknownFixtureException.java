package dev.rota.application;

import java.util.Set;
import java.util.TreeSet;

/** The fixture name is not one the use case accepts. */
public final class UnknownFixtureException extends RuntimeException {

    public UnknownFixtureException(String fixture, Set<String> allowed) {
        super("unknown fixture '" + fixture + "'; allowed: " + String.join(", ", new TreeSet<>(allowed)));
    }
}
