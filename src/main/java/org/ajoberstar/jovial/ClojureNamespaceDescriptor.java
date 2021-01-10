package org.ajoberstar.jovial.engine.clojure_test;

import java.util.Set;

import clojure.lang.Namespace;
import org.ajoberstar.jovial.lang.clojure.util.SimpleClojure;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

public class ClojureNamespaceDescriptor extends AbstractTestDescriptor {
  private final Namespace ns;
  private final Set<TestTag> tags;

  public ClojureNamespaceDescriptor(UniqueId id, Namespace ns, TestSource source, Set<TestTag> tags) {
    super(id, ns.toString(), source);
    this.ns = ns;
    this.tags = tags;
  }

  public Namespace getNamespace() {
    return ns;
  }

  @Override
  public Type getType() {
    return Type.CONTAINER;
  }

  @Override
  public Set<TestTag> getTags() {
    return tags;
  }
}
