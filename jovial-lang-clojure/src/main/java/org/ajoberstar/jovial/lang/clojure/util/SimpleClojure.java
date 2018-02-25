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
package org.ajoberstar.jovial.lang.clojure.util;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

public final class SimpleClojure {
  private SimpleClojure() {
    // don't instantiate
  }

  public static <T> T invoke(String namespace, String name, Object... args) {
    if (!"clojure.core".equals(namespace)) {
      IFn symbol = Clojure.var("clojure.core", "symbol");
      IFn require = Clojure.var("clojure.core", "require");

      Object sym = symbol.invoke(namespace);
      require.invoke(sym);
    }

    IFn apply = Clojure.var("clojure.core", "apply");
    return (T) apply.invoke(Clojure.var(namespace, name), args);
  }
}
