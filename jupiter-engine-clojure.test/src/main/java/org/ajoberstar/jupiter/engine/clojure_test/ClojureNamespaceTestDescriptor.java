package org.ajoberstar.jupiter.engine.clojure_test;

import org.junit.gen5.engine.support.descriptor.AbstractTestDescriptor;

public class ClojureNamespaceTestDescriptor extends AbstractTestDescriptor {
    public ClojureNamespaceTestDescriptor(ClojureNamespaceSource source) {
        super(source.getNamespace());
        setSource(source);
    }

    @Override
    public boolean isTest() {
        return false;
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public String getName() {
        return super.getUniqueId();
    }

    @Override
    public String getDisplayName() {
        return super.getUniqueId();
    }
}
