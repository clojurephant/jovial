(ns org.ajoberstar.jovial.lang.clojure.impl.discovery
  (:refer-clojure :exclude [filter])
  (:require [org.ajoberstar.jovial.lang.clojure :as lang]
            [clojure.tools.namespace.find :refer [find-namespaces]]
            [clojure.string :as str])
  (:import (clojure.lang Var Namespace)
           (java.io File)
           (java.nio.file Path Paths)
           (java.net URI)
           (org.ajoberstar.jovial.lang.clojure VarSelector NamespaceSelector NamespaceFilter)
           (org.junit.platform.engine UniqueId EngineDiscoveryRequest Filter DiscoverySelector)
           (org.junit.platform.engine.discovery UniqueIdSelector ClasspathRootSelector ClassSelector)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Selecting candidates for discovery
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord TestCandidate [namespace var])

(defprotocol Selector
  (-select [this] "Selects a seq of TestCandidates for discovery of tests."))

(extend-protocol Selector
  nil
  (-select [_] nil)
  Object
  (-select [_] nil)
  Var
  (-select [var] [(->TestCandidate (-> var meta :ns) var)])
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
  URI
  (-select [uri] (-select (Paths/get uri)))
  Class
  (-select [clazz]
    (let [clazz-name (.getCanonicalName clazz)
          ns-name (-> clazz-name
                      (str/replace "__init" "")
                      (str/replace "_" "-"))
          ns-sym (symbol ns-name)]
      (try
        (require ns-sym)
        (-select (find-ns ns-sym))
        (catch Exception _
          nil))))
  VarSelector
  (-select [selector] (-select (.getVar selector)))
  NamespaceSelector
  (-select [selector] (-select (.getNamespace selector)))
  UniqueIdSelector
  (-select [selector] (-select (.getUniqueId selector)))
  ClasspathRootSelector
  (-select [selector] (-select (.getClasspathRoot selector)))
  ClassSelector
  (-select [selector] (-select (.getJavaClass selector))))

(defn select [^EngineDiscoveryRequest request]
  (mapcat -select (.getSelectorsByType request DiscoverySelector)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Filtering candidates for discovery
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- ns-filter [^EngineDiscoveryRequest request candidates]
  (let [filters (.getFiltersByType request NamespaceFilter)
        unified (Filter/composeFilters filters)
        included? (fn [cand] (->> cand :namespace (.apply unified) .included))]
    (clojure.core/filter included? candidates)))

(defn filter [^EngineDiscoveryRequest request candidates]
  (ns-filter request candidates))
