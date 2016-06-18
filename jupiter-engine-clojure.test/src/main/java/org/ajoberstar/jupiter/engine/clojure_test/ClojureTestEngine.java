package org.ajoberstar.jupiter.engine.clojure_test;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Namespace;
import clojure.lang.Var;
import org.ajoberstar.jupiter.lang.clojure.NamespaceSelector;
import org.ajoberstar.jupiter.lang.clojure.VarSelector;
import org.junit.gen5.engine.EngineDiscoveryRequest;
import org.junit.gen5.engine.ExecutionRequest;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.TestEngine;
import org.junit.gen5.engine.UniqueId;
import org.junit.gen5.engine.discovery.ClasspathSelector;
import org.junit.gen5.engine.discovery.UniqueIdSelector;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClojureTestEngine implements TestEngine {
    public static final String ENGINE_ID = "clojure.test";

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        Stream<File> testDirs = discoveryRequest.getSelectorsByType(ClasspathSelector.class).stream()
            .map(ClasspathSelector::getClasspathRoot);

        Stream<Namespace> namespaces = discoveryRequest.getSelectorsByType(NamespaceSelector.class).stream()
            .map(NamespaceSelector::getNamespace);

        Stream<Var> vars = discoveryRequest.getSelectorsByType(VarSelector.class).stream()
            .map(VarSelector::getVar);

        Stream<UniqueId> uniqueIds = discoveryRequest.getSelectorsByType(UniqueIdSelector.class).stream()
            .map(UniqueIdSelector::getUniqueId);

        List<? extends Object> roots = Stream.of(testDirs, namespaces, vars, uniqueIds)
            .reduce(Stream::concat)
            .orElse(Stream.empty())
            .collect(Collectors.toList());

        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("org.ajoberstar.jupiter.engine.clojure-test.discovery"));

        IFn scanner = Clojure.var("org.ajoberstar.jupiter.engine.clojure-test.discovery", "discover-descriptor");
        TestDescriptor descriptor = (TestDescriptor) scanner.invoke(uniqueId, roots);

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
