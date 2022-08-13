package dev.clojurephant.jovial;

import static org.junit.Assert.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

public class ClojureTestEngineTest {
  @Test
  public void selectingByClass() throws ClassNotFoundException {
    Class<?> clojureClazz = Class.forName("sample.other_test__init");

    EngineDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectClass(clojureClazz))
        .build();
    UniqueId root = UniqueId.root("sample", "test");

    List<UniqueId> expectedIds = Stream.of(
        root.append("namespace", "sample.other-test"),
        root.append("namespace", "sample.other-test").append("name", "my-other-works"),
        root.append("namespace", "sample.other-test").append("name", "my-other-fails"),
        root.append("namespace", "sample.other-test").append("name", "my-other-error")).collect(Collectors.toList());

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    List<UniqueId> actualIds = descriptor.getDescendants().stream()
        .map(TestDescriptor::getUniqueId)
        .collect(Collectors.toList());

    assertEquals(expectedIds, actualIds);
  }

  @Test
  public void selectingByClasspathRoot() {
    Set<Path> roots = Arrays.stream(System.getProperty("classpath.roots").split(File.pathSeparator))
        .map(Paths::get)
        .collect(Collectors.toSet());

    EngineDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
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
        root.append("namespace", "sample.other-test").append("name", "my-other-error")).collect(Collectors.toList());

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    List<UniqueId> actualIds = descriptor.getDescendants().stream()
        .map(TestDescriptor::getUniqueId)
        .collect(Collectors.toList());

    assertEquals(expectedIds, actualIds);
  }

  @Test
  public void selectingByClasspathResource() {
    EngineDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectClasspathResource("/sample/other_test.clj"))
        .build();

    UniqueId root = UniqueId.root("sample", "test");

    List<UniqueId> expectedIds = Stream.of(
        root.append("namespace", "sample.other-test"),
        root.append("namespace", "sample.other-test").append("name", "my-other-works"),
        root.append("namespace", "sample.other-test").append("name", "my-other-fails"),
        root.append("namespace", "sample.other-test").append("name", "my-other-error"))
        .collect(Collectors.toList());

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    List<UniqueId> actualIds = descriptor.getDescendants().stream()
        .map(TestDescriptor::getUniqueId)
        .collect(Collectors.toList());

    assertEquals(expectedIds, actualIds);
  }

  @Test
  public void selectingTestByUniqueId() {
    UniqueId id = UniqueId.root("sample", "test").append("namespace", "sample.other-test").append("name", "my-other-works");
    EngineDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectUniqueId(id))
        .build();
    UniqueId root = UniqueId.root("sample", "test");

    List<UniqueId> expectedIds = Stream.of(
        root.append("namespace", "sample.other-test"),
        root.append("namespace", "sample.other-test").append("name", "my-other-works")).collect(Collectors.toList());

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    List<UniqueId> actualIds = descriptor.getDescendants().stream()
        .map(TestDescriptor::getUniqueId)
        .collect(Collectors.toList());

    assertEquals(expectedIds, actualIds);
  }

  @Test
  public void selectingContainerByUniqueId() {
    UniqueId id = UniqueId.root("sample", "test").append("namespace", "sample.other-test");
    EngineDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectUniqueId(id))
        .build();
    UniqueId root = UniqueId.root("sample", "test");

    List<UniqueId> expectedIds = Stream.of(
        root.append("namespace", "sample.other-test"),
        root.append("namespace", "sample.other-test").append("name", "my-other-works"),
        root.append("namespace", "sample.other-test").append("name", "my-other-fails"),
        root.append("namespace", "sample.other-test").append("name", "my-other-error")).collect(Collectors.toList());

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    List<UniqueId> actualIds = descriptor.getDescendants().stream()
        .map(TestDescriptor::getUniqueId)
        .collect(Collectors.toList());

    assertEquals(expectedIds, actualIds);
  }

  @Test
  public void getsTagsFromMetadata() {
    Set<Path> roots = Arrays.stream(System.getProperty("classpath.roots").split(File.pathSeparator))
        .map(Paths::get)
        .collect(Collectors.toSet());

    EngineDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(selectClasspathRoots(roots)).build();
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
    Map<UniqueId, Set<TestTag>> actualTags = descriptor.getDescendants().stream()
        .collect(Collectors.toMap(TestDescriptor::getUniqueId, TestDescriptor::getTags));

    assertEquals(expectedTags, actualTags);
  }

  private Set<TestTag> tags(String... tags) {
    return Arrays.stream(tags).map(TestTag::create).collect(Collectors.toSet());
  }
}
