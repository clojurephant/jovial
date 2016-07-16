package org.ajoberstar.jovial.engine.clojure_test;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ClojureTestDescriptor extends AbstractTestDescriptor {
    private final Set<TestTag> tags;

    public ClojureTestDescriptor(UniqueId id, String displayName, Set<TestTag> tags, TestSource source) {
        super(id, displayName);
        this.tags = tags;
        setSource(source);
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

    @Override
    public Set<TestTag> getTags() {
        return tags;
    }
}
