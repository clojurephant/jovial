package org.ajoberstar.jupiter.engine.clojure_test;

import org.ajoberstar.jupiter.lang.clojure.BaseClojureEngine;
import org.ajoberstar.jupiter.lang.clojure.util.SimpleClojure;
import org.junit.gen5.engine.ConfigurationParameters;

public class ClojureTestEngine extends BaseClojureEngine {
    public static final String ENGINE_ID = "clojure.test";

    @Override
    public String getId() {
        return ENGINE_ID;
    }

    @Override
    protected Object getEngine(ConfigurationParameters config) {
        return SimpleClojure.invoke("org.ajoberstar.jupiter.engine.clojure-test", "engine", config);
    }
}
