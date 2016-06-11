package org.ajoberstar.jupiter.engine.clojure_test;

import clojure.java.api.Clojure;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.gen5.engine.TestSource;

import java.util.Map;

public class ClojureVarSource implements TestSource {
    private final String namespace;
    private final String name;

    public ClojureVarSource(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public ClojureNamespaceSource getNamespaceSource() {
        return new ClojureNamespaceSource(namespace);
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public static ClojureVarSource fromMeta(Map<Object, Object> meta) {
        String namespace = meta.get(Clojure.read(":ns")).toString();
        String name = meta.get(Clojure.read(":name")).toString();
        return new ClojureVarSource(namespace, name);
    }

    @Override
    public boolean isJavaClass() {
        return false;
    }

    @Override
    public boolean isJavaMethod() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isFilePosition() {
        return false;
    }
}
