(ns org.ajoberstar.jovial.lang.clojure
  (:require [clojure.java.io :as io])
  (:import (clojure.lang Namespace Var)
           (java.util Optional)
           (org.junit.platform.engine TestSource TestTag UniqueId)
           (org.junit.platform.engine.support.descriptor FilePosition FileSource CompositeTestSource)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UniqueId creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *root-id*)

(defprotocol Identifiable
  (->id [this] "Creates a unique id from this object."))

(extend-protocol Identifiable
  nil
  (->id [_] nil)
  Namespace
  (->id [ns]
    (.append *root-id* "namespace" (str ns)))
  Var
  (->id [var]
    (let [ns-id (-> var meta :ns ->id)]
      (.append ns-id "name" (-> var meta :name name)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TestSource creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *root-dir* nil)

(defrecord ClojureNamespaceSource
  [namespace]
  Object
  (toString [_] (str "ClojureNamespaceSource[namespace = " namespace "]"))
  TestSource)

(defn ns-source [namespace]
  (->ClojureNamespaceSource (str namespace)))

(defrecord ClojureVarSource
  [namespace name]
  Object
  (toString [_] (str "ClojureVarSource[namespace = " namespace ", name = " name "]"))
  TestSource)

(defn var-source [var]
  (let [{:keys [ns name file line column]} (meta var)
        file-pos (if (and line column) (FilePosition/from line column))
        file (if file (FileSource/from (io/file *root-dir* file) file-pos))
        var (->ClojureVarSource (str ns) (str name))]
    (if file
      (CompositeTestSource/from [var file])
      var)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Converting to a ref
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- id->map [id]
  (let [xf (map (fn [segment] [(keyword (.getType segment)) (.getValue segment)]))]
    (into {} xf (.getSegments id))))

(defprotocol ClojureRef
  (->ref [this] "Finds the ref associated with the provided object."))

(extend-protocol ClojureRef
  nil
  (->ref [_] nil)
  ClojureNamespaceSource
  (->ref [source]
    (let [sym (symbol (:namespace source))]
      (find-ns sym)))
  ClojureVarSource
  (->ref [source]
    (let [sym (symbol (:namespace source) (:name source))]
      (find-var sym)))
  CompositeTestSource
  (->ref [comp-source]
    (let [clojure? (fn [source]
                     (if (some #(instance? % source) [ClojureVarSource ClojureNamespaceSource])
                       source))]
      (->> comp-source
           .getSources
           (some clojure?)
           ->ref)))
  UniqueId
  (->ref [id]
    (let [{:keys [namespace name]} (id->map id)]
      (if name
        (find-var (symbol namespace name))
        (find-ns (symbol namespace)))))
  Optional
  (->ref [opt]
    (if (.isPresent opt)
      (-> opt .get ->ref))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Other utilities
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:private excluded-tags #{:ns :file :line :column :doc :author :test :name})

(defn- tags [var]
  (let [var-meta (-> var meta)
        ns-meta (-> var-meta :ns meta)
        full-meta (merge ns-meta var-meta)
        xf (comp (filter second) ; if meta value is truthy, use it as a tag
                 (map first) ; just need the keyword
                 (remove excluded-tags) ; these are unlikely to be meant as a tag
                 (map name)
                 (map #(TestTag/create %)))]
    (into #{} xf full-meta)))
