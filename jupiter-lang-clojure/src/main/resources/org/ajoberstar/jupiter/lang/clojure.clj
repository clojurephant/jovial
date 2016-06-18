(ns org.ajoberstar.jupiter.lang.clojure
  (:require [clojure.java.io :as io])
  (:import (java.io File)
           (java.nio.file Path)
           (org.junit.gen5.engine.support.descriptor DirectorySource FileSource FilePosition CompositeTestSource)
           (org.junit.gen5.engine TestSource UniqueId)
           (clojure.lang Symbol Namespace Var)
           (java.util Optional)))

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
      (.append ns-id "name" (-> var meta :name name))))
  Symbol
  (->id [sym]
    (if (namespace sym)
      (->id (find-var sym))
      (do
        (require sym)
        (->id (find-ns sym))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TestSource creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *root-dir* nil)

(defrecord ClojureNamespaceSource
  [namespace]
  Object
  (toString [_] (str "ClojureNamespaceSource[namespace = " namespace "]"))
  TestSource)

(defrecord ClojureVarSource
  [namespace name]
  Object
  (toString [_] (str "ClojureVarSource[namespace = " namespace ", name = " name "]"))
  TestSource)

(defprotocol ClojureTestSource
  (->source [this] "Creates a test source from this object."))

(extend-protocol ClojureTestSource
  nil
  (->source [_] nil)
  File
  (->source [file]
    (if (.isDirectory file)
      (DirectorySource. file)
      (FileSource. file)))
  Path
  (->source [path]
    (->source (.toFile path)))
  Namespace
  (->source [ns]
    (->ClojureNamespaceSource (str ns)))
  Var
  (->source [var]
    (let [{:keys [ns name file line column]} (meta var)
          file-pos (if (and line column) (FilePosition. line column))
          file (if file (FileSource. (io/file *root-dir* file) file-pos))
          var (->ClojureVarSource (str ns) (str name))]
      (if file
        (CompositeTestSource. [var file])
        var)))
  Symbol
  (->source [sym]
    (if (namespace sym)
      (->source (find-var sym))
      (do
        (require sym)
        (->source (find-ns sym))))))

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
;; Friendly name conversion
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol Friendly
  (->friendly [this] "Converts this to a friendly name."))

(extend-protocol Friendly
  nil
  (->friendly [_] nil)
  Namespace
  (->friendly [ns] (str ns))
  Var
  (->friendly [var]
    (-> var meta :name str)))
