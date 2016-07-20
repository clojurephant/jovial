package org.ajoberstar.jovial.engine.clojure_test;

import clojure.lang.Namespace;
import java.util.Set;

import org.ajoberstar.jovial.lang.clojure.util.SimpleClojure;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

public class ClojureNamespaceDescriptor extends AbstractTestDescriptor {
    private final Namespace ns;
    private final Set<TestTag> tags;

    public ClojureNamespaceDescriptor(UniqueId id, Namespace ns) {
        super(id, ns.toString());
        this.ns = ns;
        this.tags = SimpleClojure.invoke("org.ajoberstar.jovial.lang.clojure", "tags", ns);
        setSource(SimpleClojure.invoke("org.ajoberstar.jovial.lang.clojure", "ns-source", ns));
    }

    public Namespace getNamespace() {
        return ns;
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public boolean isTest() {
        return false;
    }

    @Override
    public Set<TestTag> getTags() {
        return tags;
    }
}
