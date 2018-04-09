package org.ajoberstar.jovial.engine.clojure_test;

import clojure.lang.Namespace;
import clojure.lang.Var;
import java.util.Set;
import org.ajoberstar.jovial.lang.clojure.util.SimpleClojure;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

public final class ClojureVarDescriptor extends AbstractTestDescriptor {
  private final Var var;
  private final Set<TestTag> tags;

  public ClojureVarDescriptor(UniqueId id, Var var) {
    super(id, var.sym.getName(), SimpleClojure.invoke("org.ajoberstar.jovial.lang.clojure", "var-source", var));
    this.var = var;
    this.tags = SimpleClojure.invoke("org.ajoberstar.jovial.lang.clojure", "tags", var);
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
