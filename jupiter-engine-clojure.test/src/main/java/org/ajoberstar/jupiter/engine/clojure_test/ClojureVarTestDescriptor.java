package org.ajoberstar.jupiter.engine.clojure_test;

import org.junit.gen5.engine.support.descriptor.AbstractTestDescriptor;

public class ClojureVarTestDescriptor extends AbstractTestDescriptor {
    public ClojureVarTestDescriptor(ClojureVarSource source) {
        super(source.getNamespace() + "/" + source.getName());
        setSource(source);
    }

    @Override
    public boolean isTest() {
        return true;
    }

    @Override
    public boolean isContainer() {
        return false;
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
