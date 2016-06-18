(ns org.ajoberstar.jupiter.lang.clojure.impl.discovery
  (:refer-clojure :exclude [filter])
  (:require [org.ajoberstar.jupiter.lang.clojure :as lang]
            [clojure.tools.namespace.find :refer [find-namespaces]])
  (:import (clojure.lang Var Namespace)
           (org.junit.gen5.engine UniqueId Filter EngineDiscoveryRequest)
           (java.io File)
           (java.nio.file Path)
           (org.ajoberstar.jupiter.lang.clojure VarSelector NamespaceSelector NamespaceFilter)
           (org.junit.gen5.engine.discovery UniqueIdSelector ClasspathSelector)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Selecting candidates for discovery
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord TestCandidate [namespace var source])

(defprotocol Selector
  (-select [this] "Selects a seq of TestCandidates for discovery of tests."))

(extend-protocol Selector
  nil
  (-select [_] nil)
  Object
  (-select [_] nil)
  Var
  (-select [var] [(->TestCandidate (-> var meta :ns) var (lang/var-source var))])
  Namespace
  (-select [ns]
    (->> ns ns-interns vals (mapcat -select)))
  UniqueId
  (-select [id] (-select (lang/->ref id)))
  File
  (-select [dir]
    (binding [lang/*root-dir* dir]
      (let [sym->ns (fn [sym] (require sym) (find-ns sym))]
        (->> [dir] find-namespaces (map sym->ns) (mapcat -select)))))
  Path
  (-select [dir] (-select (.toFile dir)))
  VarSelector
  (-select [selector] (-select (.getVar selector)))
  NamespaceSelector
  (-select [selector] (-select (.getNamespace selector)))
  UniqueIdSelector
  (-select [selector] (-select (.getUniqueId selector)))
  ClasspathSelector
  (-select [selector] (-select (.getClasspathRoot selector))))

(defn select [^EngineDiscoveryRequest request]
  (mapcat -select (.getSelectors request)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Filtering candidates for discovery
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- ns-filter [request candidates]
  (let [filters (.getDiscoveryFiltersByType request NamespaceFilter)
        unified (Filter/composeFilters filters)
        included? (fn [cand] (->> cand :namespace (.apply unified) .included))]
    (clojure.core/filter included? candidates)))

(defn filter [^EngineDiscoveryRequest request candidates]
  (ns-filter request candidates))
