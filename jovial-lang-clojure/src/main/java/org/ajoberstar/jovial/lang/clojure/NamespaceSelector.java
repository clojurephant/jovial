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
package org.ajoberstar.jovial.lang.clojure;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Namespace;
import org.junit.platform.engine.DiscoverySelector;

public class NamespaceSelector implements DiscoverySelector {
  private final Namespace namespace;

  private NamespaceSelector(Namespace namespace) {
    this.namespace = namespace;
  }

  public Namespace getNamespace() {
    return namespace;
  }

  public static NamespaceSelector selectNamespace(String namespace) {
    IFn symbol = Clojure.var("clojure.core", "symbol");
    IFn require = Clojure.var("clojure.core", "require");
    IFn findNs = Clojure.var("clojure.core", "find-ns");

    Object sym = symbol.invoke(namespace);
    require.invoke(sym);

    Namespace ns = (Namespace) findNs.invoke(sym);

    return new NamespaceSelector(ns);
  }
}
