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

import clojure.lang.Namespace;
import clojure.lang.Var;
import java.util.Set;
import org.ajoberstar.jovial.lang.clojure.util.SimpleClojure;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

public class ClojureVarDescriptor extends AbstractTestDescriptor {
  private final Var var;
  private final Set<TestTag> tags;

  public ClojureVarDescriptor(UniqueId id, Var var) {
    super(id, var.sym.getName());
    this.var = var;
    this.tags = SimpleClojure.invoke("org.ajoberstar.jovial.lang.clojure", "tags", var);
    setSource(SimpleClojure.invoke("org.ajoberstar.jovial.lang.clojure", "var-source", var));
  }

  public Var getVar() {
    return var;
  }

  public Namespace getNamespace() {
    return var.ns;
  }

  @Override
  public boolean isContainer() {
    return false;
  }

  @Override
  public boolean isTest() {
    return true;
  }

  @Override
  public Set<TestTag> getTags() {
    return tags;
  }
}
