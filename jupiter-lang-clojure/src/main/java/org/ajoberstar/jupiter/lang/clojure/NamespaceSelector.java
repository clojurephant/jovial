package org.ajoberstar.jupiter.lang.clojure;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Namespace;
import org.junit.gen5.engine.DiscoverySelector;

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
