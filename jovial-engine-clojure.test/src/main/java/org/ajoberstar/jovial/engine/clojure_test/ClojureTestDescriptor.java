package org.ajoberstar.jovial.engine.clojure_test;

import org.junit.gen5.engine.TestSource;
import org.junit.gen5.engine.TestTag;
import org.junit.gen5.engine.UniqueId;
import org.junit.gen5.engine.support.descriptor.AbstractTestDescriptor;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ClojureTestDescriptor extends AbstractTestDescriptor {
    private final String displayName;
    private final Set<TestTag> tags;

    public ClojureTestDescriptor(UniqueId id, String displayName, Set<TestTag> tags, TestSource source) {
        super(id);
        this.displayName = displayName;
        this.tags = tags;
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

    @Override
    public Set<TestTag> getTags() {
        return tags;
    }
}
