package dev.clojurephant.jovial.util;

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
