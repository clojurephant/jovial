package org.ajoberstar.jupiter.engine.clojure_test;

import clojure.java.api.Clojure;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.gen5.engine.TestSource;

import java.util.Map;

public class ClojureNamespaceSource implements TestSource {
    private final String namespace;

    public ClojureNamespaceSource(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
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

    public static ClojureNamespaceSource fromMeta(Map<Object, Object> meta) {
        String namespace = meta.get(Clojure.read(":ns")).toString();
        return new ClojureNamespaceSource(namespace);
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
