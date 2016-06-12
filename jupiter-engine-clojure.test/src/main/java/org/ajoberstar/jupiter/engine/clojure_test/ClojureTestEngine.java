package org.ajoberstar.jupiter.engine.clojure_test;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import org.junit.gen5.engine.*;
import org.junit.gen5.engine.discovery.ClasspathSelector;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ClojureTestEngine implements TestEngine {
    public static final String ENGINE_ID = "clojure.test";

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest) {
        // TODO support UniqueIdSelectors
        List<File> testDirs = discoveryRequest.getSelectorsByType(ClasspathSelector.class).stream()
            .map(ClasspathSelector::getClasspathRoot)
            .collect(Collectors.toList());

        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("org.ajoberstar.jupiter.engine.clojure-test.discovery"));

        IFn scanner = Clojure.var("org.ajoberstar.jupiter.engine.clojure-test.discovery", "discover-descriptor");
        TestDescriptor descriptor = (TestDescriptor) scanner.invoke(testDirs);

        // TODO support filters

        return descriptor;
    }

    @Override
    public void execute(ExecutionRequest request) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("org.ajoberstar.jupiter.engine.clojure-test.execution"));

        IFn scanner = Clojure.var("org.ajoberstar.jupiter.engine.clojure-test.execution", "execute-tests");
        scanner.invoke(request.getRootTestDescriptor(), request.getEngineExecutionListener());
    }

    @Override
    public String getId() {
        return ENGINE_ID;
    }
}
