/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ajoberstar.jovial.engine.clojure_test;

import static org.ajoberstar.jovial.lang.clojure.NamespaceFilter.includeNamespacePattern;
import static org.ajoberstar.jovial.lang.clojure.NamespaceSelector.selectNamespace;
import static org.ajoberstar.jovial.lang.clojure.VarSelector.selectVar;
import static org.junit.Assert.*;
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
  public void selectingByClasspathDir() {
    Set<Path> roots =
        Arrays.stream(System.getProperty("classpath.roots").split(File.pathSeparator))
            .map(Paths::get)
            .collect(Collectors.toSet());

    EngineDiscoveryRequest request =
        LauncherDiscoveryRequestBuilder.request().selectors(selectClasspathRoots(roots)).build();
    UniqueId root = UniqueId.root("sample", "test");

    List<UniqueId> expectedIds =
        Stream.of(
                root.append("namespace", "sample.core-test"),
                root.append("namespace", "sample.other-test"),
                root.append("namespace", "sample.core-test").append("name", "my-sample-works"),
                root.append("namespace", "sample.core-test").append("name", "my-sample-fails"),
                root.append("namespace", "sample.other-test").append("name", "my-other-works"),
                root.append("namespace", "sample.other-test").append("name", "my-other-fails"),
                root.append("namespace", "sample.other-test").append("name", "my-other-error"))
            .collect(Collectors.toList());

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    List<UniqueId> actualIds =
        descriptor
            .getDescendants()
            .stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

    assertEquals(expectedIds, actualIds);
  }

  @Test
  public void selectingByNamespace() {
    EngineDiscoveryRequest request =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(selectNamespace("sample.other-test"))
            .build();
    UniqueId root = UniqueId.root("sample", "test");

    List<UniqueId> expectedIds =
        Stream.of(
                root.append("namespace", "sample.other-test"),
                root.append("namespace", "sample.other-test").append("name", "my-other-works"),
                root.append("namespace", "sample.other-test").append("name", "my-other-fails"),
                root.append("namespace", "sample.other-test").append("name", "my-other-error"))
            .collect(Collectors.toList());

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    List<UniqueId> actualIds =
        descriptor
            .getDescendants()
            .stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

    assertEquals(expectedIds, actualIds);
  }

  @Test
  public void selectingByVar() {
    EngineDiscoveryRequest request =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(selectVar("sample.other-test", "my-other-works"))
            .build();
    UniqueId root = UniqueId.root("sample", "test");

    List<UniqueId> expectedIds =
        Stream.of(
                root.append("namespace", "sample.other-test"),
                root.append("namespace", "sample.other-test").append("name", "my-other-works"))
            .collect(Collectors.toList());

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    List<UniqueId> actualIds =
        descriptor
            .getDescendants()
            .stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

    assertEquals(expectedIds, actualIds);
  }

  @Test
  public void selectingTestByUniqueId() {
    EngineDiscoveryRequest request =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(
                selectUniqueId(
                    UniqueId.root("sample", "test")
                        .append("namespace", "sample.other-test")
                        .append("name", "my-other-works")))
            .build();
    UniqueId root = UniqueId.root("sample", "test");

    List<UniqueId> expectedIds =
        Stream.of(
                root.append("namespace", "sample.other-test"),
                root.append("namespace", "sample.other-test").append("name", "my-other-works"))
            .collect(Collectors.toList());

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    List<UniqueId> actualIds =
        descriptor
            .getDescendants()
            .stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

    assertEquals(expectedIds, actualIds);
  }

  @Test
  public void selectingContainerByUniqueId() {
    EngineDiscoveryRequest request =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(
                selectUniqueId(
                    UniqueId.root("sample", "test").append("namespace", "sample.other-test")))
            .build();
    UniqueId root = UniqueId.root("sample", "test");

    List<UniqueId> expectedIds =
        Stream.of(
                root.append("namespace", "sample.other-test"),
                root.append("namespace", "sample.other-test").append("name", "my-other-works"),
                root.append("namespace", "sample.other-test").append("name", "my-other-fails"),
                root.append("namespace", "sample.other-test").append("name", "my-other-error"))
            .collect(Collectors.toList());

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    List<UniqueId> actualIds =
        descriptor
            .getDescendants()
            .stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

    assertEquals(expectedIds, actualIds);
  }

  @Test
  public void filteringByNamespace() {
    Set<Path> roots =
        Arrays.stream(System.getProperty("classpath.roots").split(File.pathSeparator))
            .map(Paths::get)
            .collect(Collectors.toSet());

    EngineDiscoveryRequest request =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClasspathRoots(roots))
            .filters(includeNamespacePattern(".*other.*"))
            .build();
    UniqueId root = UniqueId.root("sample", "test");

    List<UniqueId> expectedIds =
        Stream.of(
                root.append("namespace", "sample.other-test"),
                root.append("namespace", "sample.other-test").append("name", "my-other-works"),
                root.append("namespace", "sample.other-test").append("name", "my-other-fails"),
                root.append("namespace", "sample.other-test").append("name", "my-other-error"))
            .collect(Collectors.toList());

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    List<UniqueId> actualIds =
        descriptor
            .getDescendants()
            .stream()
            .map(TestDescriptor::getUniqueId)
            .collect(Collectors.toList());

    assertEquals(expectedIds, actualIds);
  }

  @Test
  public void getsTagsFromMetadata() {
    Set<Path> roots =
        Arrays.stream(System.getProperty("classpath.roots").split(File.pathSeparator))
            .map(Paths::get)
            .collect(Collectors.toSet());

    EngineDiscoveryRequest request =
        LauncherDiscoveryRequestBuilder.request().selectors(selectClasspathRoots(roots)).build();
    UniqueId root = UniqueId.root("sample", "test");

    Map<UniqueId, Set<TestTag>> expectedTags = new HashMap<>();
    expectedTags.put(root.append("namespace", "sample.core-test"), tags("integration"));
    expectedTags.put(root.append("namespace", "sample.other-test"), tags());
    expectedTags.put(
        root.append("namespace", "sample.core-test").append("name", "my-sample-works"),
        tags("integration"));
    expectedTags.put(
        root.append("namespace", "sample.core-test").append("name", "my-sample-fails"), tags());
    expectedTags.put(
        root.append("namespace", "sample.other-test").append("name", "my-other-works"),
        tags("unit"));
    expectedTags.put(
        root.append("namespace", "sample.other-test").append("name", "my-other-fails"), tags());
    expectedTags.put(
        root.append("namespace", "sample.other-test").append("name", "my-other-error"),
        tags("integration"));

    TestDescriptor descriptor = new ClojureTestEngine().discover(request, root);
    Map<UniqueId, Set<TestTag>> actualTags =
        descriptor
            .getDescendants()
            .stream()
            .collect(Collectors.toMap(TestDescriptor::getUniqueId, TestDescriptor::getTags));

    assertEquals(expectedTags, actualTags);
  }

  private Set<TestTag> tags(String... tags) {
    return Arrays.stream(tags).map(TestTag::create).collect(Collectors.toSet());
  }
}
