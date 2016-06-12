(ns org.ajoberstar.jupiter.engine.clojure-test.discovery
  (:require [clojure.tools.namespace.find :refer [find-namespaces]])
  (:import (clojure.lang Var Symbol Namespace)
           (java.io File)
           (java.nio.file Path)
           (org.ajoberstar.jupiter.engine.clojure_test ClojureVarSource ClojureVarTestDescriptor ClojureNamespaceSource ClojureNamespaceTestDescriptor ClojureTestEngine)
           (org.junit.gen5.engine.support.descriptor EngineDescriptor)))

(defprotocol Discoverable
  (-discover-tests [this] "Return a seqable? of any test vars discovered in this."))

(extend-protocol Discoverable
  nil
  (-discover-tests [_] nil)
  Var
  (-discover-tests [var]
    (if (-> var meta :test)
      [var]))
  Symbol
  (-discover-tests [sym]
    (if (namespace sym)
      (-discover-tests (find-var sym))
      (-discover-tests
        (do
          (require sym)
          (find-ns sym)))))
  Namespace
  (-discover-tests [ns]
    (->> ns ns-interns vals (mapcat -discover-tests)))
  File
  (-discover-tests [file]
    (mapcat -discover-tests (find-namespaces [file])))
  Path
  (-discover-tests [path]
    (-discover-tests (.toFile path))))

(defn- ns->descriptor [[ns vars]]
  (let [ns-desc (-> ns str ClojureNamespaceSource. ClojureNamespaceTestDescriptor.)
        var->descriptor #(-> % meta ClojureVarSource/fromMeta ClojureVarTestDescriptor.)
        var-descs (map var->descriptor vars)]
    (doseq [var-desc var-descs]
      (.addChild ns-desc var-desc))
    ns-desc))

(defn discover-descriptor [roots]
  (let [engine-desc (EngineDescriptor. ClojureTestEngine/ENGINE_ID ClojureTestEngine/ENGINE_ID)
        ns-descs (->> roots
                      (mapcat -discover-tests)
                      (group-by (comp :ns meta))
                      (map ns->descriptor))]
    (doseq [ns-desc ns-descs]
      (.addChild engine-desc ns-desc))
    engine-desc))
