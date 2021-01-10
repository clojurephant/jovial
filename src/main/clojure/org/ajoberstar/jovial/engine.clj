(ns org.ajoberstar.jovial.engine
  (:refer-clojure :exclude [descriptor])
  (:require [clojure.java.io :as io]
            [clojure.main :as main]
            [clojure.string :as string]
            [clojure.tools.namespace.file :as ns-file]
            [clojure.tools.namespace.find :as ns-find]
            [clojure.tools.namespace.parse :as ns-parse])
  (:import [java.io File]
           [java.nio.file Paths]
           [org.ajoberstar.jovial ClojureNamespaceDescriptor ClojureVarDescriptor]
           [org.junit.platform.engine
            DiscoverySelector EngineDiscoveryListener EngineDiscoveryRequest ExecutionRequest
            SelectorResolutionResult TestTag UniqueId UniqueId$Segment]
           [org.junit.platform.engine.discovery
            ClasspathResourceSelector ClasspathRootSelector ClassSelector UniqueIdSelector]
           [org.junit.platform.engine.support.descriptor
            EngineDescriptor ClasspathResourceSource ClassSource]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Specification of an Engine
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol Engine
  (id [engine]
    "Returns the UniqueId of the engine")
  (discover [engine ^EngineDiscoveryRequest request ^UniqueId root-id]
    "Discovers tests among the candidates, should return a test descriptor.")
  (execute [engine ^ExecutionRequest request]
    "Executes the tests in the request's descriptor and reports results to the request's listener."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utilities
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:private excluded-tags #{:ns :file :line :column :doc :author :test :name})

(defn tags [var]
  (let [var-meta (-> var meta)
        ns-meta (-> var-meta :ns meta)
        full-meta (merge ns-meta var-meta)
        xf (comp (filter second) ; if meta value is truthy, use it as a tag
                 (map first) ; just need the keyword
                 (remove excluded-tags) ; these are unlikely to be meant as a tag
                 (map name)
                 (map #(TestTag/create %)))]
    (into #{} xf full-meta)))

(defn- id->map [^UniqueId id]
  (let [xf (map (fn [^UniqueId$Segment segment]
                  [(keyword (.getType segment)) (.getValue segment)]))]
    (into {} xf (.getSegments id))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Discovery Selector Support
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord TestCandidate [source sym])

(defn ns-vars [ns]
  (let [ns-str (name (ns-name ns))]
    (map (fn [[sym _]]
           (symbol ns-str (name sym)))
         (ns-publics ns))))

(defn select-new-vars [source ns-sym]
  (try
    (require ns-sym)
    (map #(->TestCandidate source %) (ns-vars (find-ns ns-sym)))
    (catch Exception e
      (println "Failure loading source" source ": " (-> e Throwable->map main/ex-triage main/ex-str))
      nil)))

(defprotocol Selector
  (-select [this]
    "Builds a test descriptor meeting the selector's criteria."))

(extend-protocol Selector
  UniqueIdSelector
  (-select [this]
    (let [{:keys [namespace name]} (id->map (.getUniqueId this))
          ns-sym (symbol namespace)
          var-sym (when name (symbol namespace name))
          result (select-new-vars nil ns-sym)]
      (if var-sym
        (filter #(= var-sym (:sym %)) result)
        result)))

  ClassSelector
  (-select [this]
    (let [name (.getClassName this)
          ns-name (-> name
                      (string/replace "__init" "")
                      (string/replace "_" "-"))
          ns-sym (symbol ns-name)
          source (ClassSource/from name)]
      (when (string/ends-with? name "__init")
        (select-new-vars source ns-sym))))

  ClasspathResourceSelector
  (-select [this]
    (let [name (.getClasspathResourceName this)
          url (io/resource name)
          ns-decl (ns-file/read-file-ns-decl url)
          ns-sym (ns-parse/name-from-ns-decl ns-decl)
          source (ClasspathResourceSource/from name)]
      (select-new-vars source ns-sym)))

  ClasspathRootSelector
  (-select [this]
    (let [uri (.getClasspathRoot this)
          path (Paths/get uri)]
      (mapcat (fn [source]
                (let [ns-decl (ns-file/read-file-ns-decl source)
                      ns-sym (ns-parse/name-from-ns-decl ns-decl)
                      rel-path (.relativize path (.toPath ^File source))
                      source (ClasspathResourceSource/from (str "/" rel-path))]
                  (select-new-vars source ns-sym)))
              (ns-find/find-sources-in-dir (.toFile path))))))

(defn try-select [^EngineDiscoveryListener listener id selector]
  (if (satisfies? Selector selector)
    (try
      (let [result (-select selector)]
        (.selectorProcessed listener id selector (SelectorResolutionResult/resolved))
        result)
      (catch Exception e
        (.selectorProcessed listener id selector (SelectorResolutionResult/failed e))))
    (.selectorProcessed listener id selector (SelectorResolutionResult/unresolved))))

(defn select [^EngineDiscoveryRequest request ^UniqueId id]
  (let [listener (.getDiscoveryListener request)
        selectors (.getSelectorsByType request DiscoverySelector)]
    (loop [result []
           head (first selectors)
           tail (rest selectors)]
      (let [candidates (try-select listener id head)]
        (if (seq tail)
          (recur (concat result candidates) (first tail) (rest tail))
          (concat result candidates))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Discovery Descriptor Support
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- var->descriptor [^UniqueId parent-id {:keys [source sym]}]
  (let [var-id (.append parent-id "name" (name sym))
        var (find-var sym)]
    (ClojureVarDescriptor. var-id var source (tags var))))

(defn- ns->descriptor [^UniqueId parent-id [ns-str candidates]]
  (let [ns-id (.append parent-id "namespace" ns-str)
        source (->> candidates
                    (group-by :source)
                    (apply max-key second)
                    first)
        ns (find-ns (symbol ns-str))
        ns-desc (ClojureNamespaceDescriptor. ns-id ns source (tags ns))]
    (doseq [var-desc (map #(var->descriptor ns-id %) candidates)]
      (.addChild ns-desc var-desc))
    ns-desc))

(defn selections->descriptor [engine ^UniqueId root-id candidates]
  (let [engine-desc (EngineDescriptor. root-id (id engine))
        ns-descs (->> candidates
                      (group-by (comp namespace :sym))
                      (map #(ns->descriptor root-id %)))]
    (doseq [ns-desc ns-descs]
      (.addChild engine-desc ns-desc))
    engine-desc))
