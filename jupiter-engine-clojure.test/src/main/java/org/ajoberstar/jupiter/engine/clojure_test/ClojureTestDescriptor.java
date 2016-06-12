package org.ajoberstar.jupiter.engine.clojure_test;

import org.junit.gen5.engine.TestSource;
import org.junit.gen5.engine.UniqueId;
import org.junit.gen5.engine.support.descriptor.AbstractTestDescriptor;

import java.util.Objects;
import java.util.Optional;

public class ClojureTestDescriptor extends AbstractTestDescriptor {
    private final String displayName;

    public ClojureTestDescriptor(UniqueId id, String displayName, TestSource source) {
        super(id);
        this.displayName = displayName;
        setSource(source);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isContainer() {
        return !isTest();
    }

    @Override
    public boolean isTest() {
        return findSegment("name").isPresent();
    }

    private Optional<String> findSegment(String type) {
        Objects.requireNonNull(type, "Type cannot be null.");
        return getUniqueId().getSegments().stream()
            .filter(segment -> type.equals(segment.getType()))
            .findAny()
            .map(UniqueId.Segment::getValue);
    }
}
