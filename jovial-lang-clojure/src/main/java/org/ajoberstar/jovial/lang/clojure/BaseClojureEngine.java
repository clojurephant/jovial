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
