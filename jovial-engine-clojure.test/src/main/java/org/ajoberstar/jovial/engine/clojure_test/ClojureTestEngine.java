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
