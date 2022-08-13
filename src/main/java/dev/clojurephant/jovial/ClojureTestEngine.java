package dev.clojurephant.jovial;

import dev.clojurephant.jovial.util.SimpleClojure;
import org.junit.platform.engine.ConfigurationParameters;

public class ClojureTestEngine extends BaseClojureEngine {
  public static final String ENGINE_ID = "dev.clojurephant.jovial.clojure-test";

  @Override
  public String getId() {
    return ENGINE_ID;
  }

  @Override
  protected Object getEngine(ConfigurationParameters config) {
    return SimpleClojure.invoke("dev.clojurephant.jovial.engine.clojure-test", "engine", config);
  }
}
