package org.ajoberstar.jovial.lang.clojure;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Namespace;
import clojure.lang.Var;
import org.junit.platform.engine.DiscoverySelector;

public class VarSelector implements DiscoverySelector {
  private final Var var;

  private VarSelector(Var var) {
    this.var = var;
  }

  public Namespace getNamespace() {
    return var.ns;
  }

  public Var getVar() {
    return var;
  }

  public static VarSelector selectVar(String namespace, String name) {
    IFn symbol = Clojure.var("clojure.core", "symbol");
    IFn require = Clojure.var("clojure.core", "require");
    IFn findVar = Clojure.var("clojure.core", "find-var");

    require.invoke(symbol.invoke(namespace));

    Object sym = symbol.invoke(namespace, name);
    Var var = (Var) findVar.invoke(sym);

    return new VarSelector(var);
  }
}
