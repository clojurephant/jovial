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

import org.ajoberstar.jovial.lang.clojure.BaseClojureEngine;
import org.ajoberstar.jovial.lang.clojure.util.SimpleClojure;
import org.junit.platform.engine.ConfigurationParameters;

public class ClojureTestEngine extends BaseClojureEngine {
  public static final String ENGINE_ID = "clojure.test";

  @Override
  public String getId() {
    return ENGINE_ID;
  }

  @Override
  protected Object getEngine(ConfigurationParameters config) {
    return SimpleClojure.invoke("org.ajoberstar.jovial.engine.clojure-test", "engine", config);
  }
}
