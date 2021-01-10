package org.ajoberstar.jovial.engine.clojure_test;

import java.util.Set;

import clojure.lang.Namespace;
import clojure.lang.Var;
import org.ajoberstar.jovial.lang.clojure.util.SimpleClojure;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

public final class ClojureVarDescriptor extends AbstractTestDescriptor {
  private final Var var;
  private final Set<TestTag> tags;

  public ClojureVarDescriptor(UniqueId id, Var var, TestSource source, Set<TestTag> tags) {
    super(id, var.sym.getName(), source);
    this.var = var;
    this.tags = tags;
  }

  public Var getVar() {
    return var;
  }

  public Namespace getNamespace() {
    return var.ns;
  }

  @Override
  public Type getType() {
    return Type.TEST;
  }

  @Override
  public Set<TestTag> getTags() {
    return tags;
  }
}
