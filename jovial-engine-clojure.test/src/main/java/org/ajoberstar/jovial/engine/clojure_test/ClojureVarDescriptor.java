package org.ajoberstar.jovial.engine.clojure_test;

import clojure.lang.Namespace;
import clojure.lang.Var;
import java.util.Set;

import org.ajoberstar.jovial.lang.clojure.util.SimpleClojure;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

public class ClojureVarDescriptor extends AbstractTestDescriptor {
    private final Var var;
    private final Set<TestTag> tags;

    public ClojureVarDescriptor(UniqueId id, Var var) {
        super(id, var.sym.getName());
        this.var = var;
        this.tags = SimpleClojure.invoke("org.ajoberstar.jovial.lang.clojure", "tags", var);
        setSource(SimpleClojure.invoke("org.ajoberstar.jovial.lang.clojure", "var-source", var));
    }

    public Var getVar() {
        return var;
    }

    public Namespace getNamespace() {
        return var.ns;
    }

    @Override
    public boolean isContainer() {
        return false;
    }

    @Override
    public boolean isTest() {
        return true;
    }

    @Override
    public Set<TestTag> getTags() {
        return tags;
    }
}
