package org.ajoberstar.jovial.lang.clojure;

import org.ajoberstar.jovial.lang.clojure.util.SimpleClojure;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

public abstract class BaseClojureEngine implements TestEngine {
  private static final String ENGINE_NS = "org.ajoberstar.jovial.lang.clojure.engine";

  @Override
  public Optional<String> getGroupId() {
    return Optional.of("org.ajoberstar");
  }

  // artifact id and version are supposed to come from the JAR manifest

  @Override
  public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
    Object engine = getEngine(request.getConfigurationParameters());
    return (TestDescriptor) SimpleClojure.invoke(ENGINE_NS, "discover", engine, request, uniqueId);
  }

  @Override
  public void execute(ExecutionRequest request) {
    Object engine = getEngine(request.getConfigurationParameters());
    SimpleClojure.invoke(ENGINE_NS, "execute", engine, request);
  }

  protected abstract Object getEngine(ConfigurationParameters config);
}
