package org.ajoberstar.jovial.lang.clojure;

import org.ajoberstar.jovial.lang.clojure.util.SimpleClojure;
import org.junit.gen5.engine.ConfigurationParameters;
import org.junit.gen5.engine.EngineDiscoveryRequest;
import org.junit.gen5.engine.ExecutionRequest;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.TestEngine;
import org.junit.gen5.engine.UniqueId;

public abstract class BaseClojureEngine implements TestEngine {
    private static final String ENGINE_NS = "org.ajoberstar.jovial.lang.clojure.engine";

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
