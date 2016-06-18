(ns org.ajoberstar.jupiter.engine.clojure-test.discovery
  (:require [clojure.tools.namespace.find :refer [find-namespaces]]
            [clojure.java.io :as io]
            [org.ajoberstar.jupiter.lang.clojure :as lang])
  (:import (clojure.lang Var Symbol Namespace)
           (java.io File)
           (java.nio.file Path)
           (org.ajoberstar.jupiter.engine.clojure_test ClojureTestEngine ClojureTestDescriptor)
           (org.junit.gen5.engine.support.descriptor EngineDescriptor)
           (org.junit.gen5.engine UniqueId)))

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
    (-discover-tests (.toFile path)))
  UniqueId
  (-discover-tests [id]
    (-discover-tests (lang/->ref id))))

(defn- ns->descriptor [[ns vars]]
  (let [->descriptor (fn [x] (ClojureTestDescriptor. (lang/->id x) (lang/->friendly x) (lang/->source x)))
        ns-desc (->descriptor ns)
        var-descs (map ->descriptor vars)]
    (doseq [var-desc var-descs]
      (.addChild ns-desc var-desc))
    ns-desc))

(defn discover-descriptor [root-id roots]
  (binding [lang/*root-id* root-id]
    (let [engine-desc (EngineDescriptor. root-id ClojureTestEngine/ENGINE_ID)]
      (doseq [root roots]
        (binding [lang/*root-dir* (if (satisfies? io/Coercions root) root)]
          (let [ns-descs (->> (-discover-tests root)
                              (group-by (comp :ns meta))
                              (map ns->descriptor))]
            (doseq [ns-desc ns-descs]
              (.addChild engine-desc ns-desc)))))
      engine-desc)))
