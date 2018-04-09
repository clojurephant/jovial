package org.ajoberstar.jovial.lang.clojure;

import clojure.lang.Namespace;
import java.util.regex.Pattern;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.FilterResult;

public class NamespaceFilter implements DiscoveryFilter<Namespace> {
  private final Pattern pattern;

  private NamespaceFilter(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  public FilterResult apply(Namespace namespace) {
    String name = namespace.getName().getName();
    return FilterResult.includedIf(pattern.matcher(name).matches());
  }

  public static NamespaceFilter includeNamespacePattern(String pattern) {
    return new NamespaceFilter(Pattern.compile(pattern));
  }
}
