(ns org.ajoberstar.jovial.lang.clojure.impl.discovery
  (:refer-clojure :exclude [filter])
  (:require [org.ajoberstar.jovial.lang.clojure :as lang]
            [clojure.tools.namespace.find :refer [find-namespaces]]
            [clojure.string :as str])
  (:import (clojure.lang Var Namespace)
           (java.io File)
           (java.nio.file Path)
           (org.ajoberstar.jovial.lang.clojure VarSelector NamespaceSelector NamespaceFilter)
           (org.junit.platform.engine UniqueId EngineDiscoveryRequest Filter DiscoverySelector)
           (org.junit.platform.engine.discovery UniqueIdSelector ClasspathSelector ClassSelector)))

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
  Class
  (-select [clazz]
    (let [clazz-name (.getCanonicalName clazz)
          ns-path (str "/" (str/replace clazz-name "." "/"))
          ns-for-class? (fn [ns]
                          (let [ns-name (-> ns str (str/replace "-" "_"))]
                            (if (= ns-name clazz-name)
                              ns)))]
      (try
        (load ns-path)
        (-select (some ns-for-class? (all-ns)))
        (catch Exception _
          nil))))
  VarSelector
  (-select [selector] (-select (.getVar selector)))
  NamespaceSelector
  (-select [selector] (-select (.getNamespace selector)))
  UniqueIdSelector
  (-select [selector] (-select (.getUniqueId selector)))
  ClasspathSelector
  (-select [selector] (-select (.getClasspathRoot selector)))
  ClassSelector
  (-select [selector] (-select (.getJavaClass selector))))


(defn select [^EngineDiscoveryRequest request]
  (mapcat -select (.getSelectorsByType request DiscoverySelector)))

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
