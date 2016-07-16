package org.ajoberstar.jovial.engine.clojure_test;

import org.junit.Test;
import org.junit.gen5.engine.EngineDiscoveryRequest;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.TestTag;
import org.junit.gen5.engine.UniqueId;
import org.junit.gen5.launcher.main.TestDiscoveryRequestBuilder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.junit.gen5.engine.discovery.ClasspathSelector.selectClasspathRoots;
import static org.junit.gen5.engine.discovery.UniqueIdSelector.selectUniqueId;
import static org.ajoberstar.jovial.lang.clojure.NamespaceSelector.selectNamespace;
import static org.ajoberstar.jovial.lang.clojure.NamespaceFilter.includeNamespacePattern;
import static org.ajoberstar.jovial.lang.clojure.VarSelector.selectVar;

public class ClojureTestEngineTest {
    @Test
    public void selectingByClasspathDir() {
        Set<File> roots = Arrays.stream(System.getProperty("classpath.roots").split(File.pathSeparator))
            .map(File::new)
            .collect(Collectors.toSet());

        EngineDiscoveryRequest request = TestDiscoveryRequestBuilder.request()
            .selectors(selectClasspathRoots(roots))
            .build();
        UniqueId root = UniqueId.root("sample", "test");

        List<UniqueId> expectedIds = Stream.of(
            root.append("namespace", "sample.core-test"),
            root.append("namespace", "sample.other-test"),
            root.append("namespace", "sample.core-test").append("name", "my-sample-works"),
            root.append("namespace", "sample.core-test").append("name", "my-sample-fails"),
            root.append("namespace", "sample.other-test").append("name", "my-other-works"),
            root.append("namespace", "sample.other-test").append("name", "my-other-fails"),
            root.append("namespace", "sample.other-test").append("name", "my-other-error")
        ).collect(Collectors.toList());

        TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
        List<UniqueId> actualIds = descriptor.getAllDescendants().stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

        assertEquals(expectedIds, actualIds);
    }

    @Test
    public void selectingByNamespace() {
        EngineDiscoveryRequest request = TestDiscoveryRequestBuilder.request()
            .selectors(selectNamespace("sample.other-test"))
            .build();
        UniqueId root = UniqueId.root("sample", "test");

        List<UniqueId> expectedIds = Stream.of(
            root.append("namespace", "sample.other-test"),
            root.append("namespace", "sample.other-test").append("name", "my-other-works"),
            root.append("namespace", "sample.other-test").append("name", "my-other-fails"),
            root.append("namespace", "sample.other-test").append("name", "my-other-error")
        ).collect(Collectors.toList());

        TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
        List<UniqueId> actualIds = descriptor.getAllDescendants().stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

        assertEquals(expectedIds, actualIds);
    }


    @Test
    public void selectingByVar() {
        EngineDiscoveryRequest request = TestDiscoveryRequestBuilder.request()
            .selectors(selectVar("sample.other-test", "my-other-works"))
            .build();
        UniqueId root = UniqueId.root("sample", "test");

        List<UniqueId> expectedIds = Stream.of(
            root.append("namespace", "sample.other-test"),
            root.append("namespace", "sample.other-test").append("name", "my-other-works")
        ).collect(Collectors.toList());

        TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
        List<UniqueId> actualIds = descriptor.getAllDescendants().stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

        assertEquals(expectedIds, actualIds);
    }

    @Test
    public void selectingTestByUniqueId() {
        EngineDiscoveryRequest request = TestDiscoveryRequestBuilder.request()
            .selectors(selectUniqueId(UniqueId.root("sample", "test").append("namespace", "sample.other-test").append("name", "my-other-works")))
            .build();
        UniqueId root = UniqueId.root("sample", "test");

        List<UniqueId> expectedIds = Stream.of(
            root.append("namespace", "sample.other-test"),
            root.append("namespace", "sample.other-test").append("name", "my-other-works")
        ).collect(Collectors.toList());

        TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
        List<UniqueId> actualIds = descriptor.getAllDescendants().stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

        assertEquals(expectedIds, actualIds);
    }

    @Test
    public void selectingContainerByUniqueId() {
        EngineDiscoveryRequest request = TestDiscoveryRequestBuilder.request()
            .selectors(selectUniqueId(UniqueId.root("sample", "test").append("namespace", "sample.other-test")))
            .build();
        UniqueId root = UniqueId.root("sample", "test");

        List<UniqueId> expectedIds = Stream.of(
            root.append("namespace", "sample.other-test"),
            root.append("namespace", "sample.other-test").append("name", "my-other-works"),
            root.append("namespace", "sample.other-test").append("name", "my-other-fails"),
            root.append("namespace", "sample.other-test").append("name", "my-other-error")
        ).collect(Collectors.toList());

        TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
        List<UniqueId> actualIds = descriptor.getAllDescendants().stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

        assertEquals(expectedIds, actualIds);
    }

    @Test
    public void filteringByNamespace() {
        Set<File> roots = Arrays.stream(System.getProperty("classpath.roots").split(File.pathSeparator))
            .map(File::new)
            .collect(Collectors.toSet());

        EngineDiscoveryRequest request = TestDiscoveryRequestBuilder.request()
            .selectors(selectClasspathRoots(roots))
            .filters(includeNamespacePattern(".*other.*"))
            .build();
        UniqueId root = UniqueId.root("sample", "test");

        List<UniqueId> expectedIds = Stream.of(
            root.append("namespace", "sample.other-test"),
            root.append("namespace", "sample.other-test").append("name", "my-other-works"),
            root.append("namespace", "sample.other-test").append("name", "my-other-fails"),
            root.append("namespace", "sample.other-test").append("name", "my-other-error")
        ).collect(Collectors.toList());

        TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
        List<UniqueId> actualIds = descriptor.getAllDescendants().stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

        assertEquals(expectedIds, actualIds);
    }

    @Test
    public void getsTagsFromMetadata() {
        Set<File> roots = Arrays.stream(System.getProperty("classpath.roots").split(File.pathSeparator))
            .map(File::new)
            .collect(Collectors.toSet());

        EngineDiscoveryRequest request = TestDiscoveryRequestBuilder.request()
            .selectors(selectClasspathRoots(roots))
            .build();
        UniqueId root = UniqueId.root("sample", "test");

        Map<UniqueId, Set<TestTag>> expectedTags = new HashMap<>();
        expectedTags.put(root.append("namespace", "sample.core-test"), tags("integration"));
        expectedTags.put(root.append("namespace", "sample.other-test"), tags());
        expectedTags.put(root.append("namespace", "sample.core-test").append("name", "my-sample-works"), tags("integration"));
        expectedTags.put(root.append("namespace", "sample.core-test").append("name", "my-sample-fails"), tags());
        expectedTags.put(root.append("namespace", "sample.other-test").append("name", "my-other-works"), tags("unit"));
        expectedTags.put(root.append("namespace", "sample.other-test").append("name", "my-other-fails"), tags());
        expectedTags.put(root.append("namespace", "sample.other-test").append("name", "my-other-error"), tags("integration"));

        TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
        Map<UniqueId, Set<TestTag>> actualTags = descriptor.getAllDescendants().stream()
            .collect(Collectors.toMap(TestDescriptor::getUniqueId, TestDescriptor::getTags));

        assertEquals(expectedTags, actualTags);
    }

    private Set<TestTag> tags(String... tags) {
        return Arrays.stream(tags)
            .map(TestTag::of)
            .collect(Collectors.toSet());
    }
}
