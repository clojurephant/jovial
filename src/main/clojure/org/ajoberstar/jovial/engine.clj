(ns org.ajoberstar.jovial.engine
  (:refer-clojure :exclude [descriptor])
  (:require [clojure.set :as set]
            [clojure.string :as string])
  (:import [org.junit.platform.engine
            DiscoverySelector EngineDiscoveryRequest ExecutionRequest
            SelectorResolutionResult UniqueId UniqueId$Segment]
           [org.junit.platform.engine.discovery
            UniqueIdSelector DirectorySelector FileSelector
            ClasspathResourceSelector ClasspathRootSelector ClassSelector]
           [org.junit.platform.engine.support.descriptor
            EngineDescriptor ClasspathResourceSource ClassSource FileSource]))

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
;; Tag creation
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UniqueId creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *root-id*)

(defn- id->map [^UniqueId id]
  (let [xf (map (fn [^UniqueId$Segment segment]
                  [(keyword (.getType segment)) (.getValue segment)]))]
    (into {} xf (.getSegments id))))

(defprotocol Identifiable
  (->id ^UniqueId [this] "Creates a unique id from this object."))

(extend-protocol Identifiable
  nil
  (->id [_] nil)
  Namespace
  (->id [ns]
    (.append ^UniqueId *root-id* "namespace" (str ns)))
  Var
  (->id [var]
    (let [ns-id (-> var meta :ns ->id)]
      (.append ns-id "name" (-> var meta :name name)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Discovery Selector Support
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *all-vars* nil)

(defn all-vars []
  (let [fqsym (fn [namespace]
                (fn [[sym _]] (symbol (name namespace) (name sym))))
        nssyms (fn [namespace]
                 (map (fqsym namespace) (ns-publics namespace)))]
    (into #{} (mapcat nssyms) (all-ns))))

(defrecord TestCandidate [source sym])

(defn select-new-vars [source loader]
  (let [before @*all-vars*]
    (try
      (loader)
      (let [after (reset! *all-vars* (all-vars))
            loaded-vars (set/difference after before)]
        (map #(->TestCandidate source %) loaded-vars))
      (catch Exception _
        nil))))

(defprotocol Selector
  (-select [this]
    "Builds a test descriptor meeting the selector's criteria."))

(extend-protocol Selector
  Object
  (-select [this]
    nil)

  UniqueIdSelector
  (-select [this parent-node]
    (let [{:keys [namespace name]} (id->map (.getUniqueId this))]
      (when (and namespace name)
        (->TestCandidate nil (symbol namespace name)))))

  FileSelector
  (-select [this parent-node]
    (let [path (str (.getPath this))
          source (FileSource/from (.getFile this))]
      (when (or (string/ends-with? path ".clj")
                (string/ends-with? path ".cljc"))
        (select-new-vars source (fn [] (load-file path))))))
  DirectorySelector
  (-select [this]
    ;; TODO implement
    nil)

  ClasspathResourceSelector
  (-select [this]
    (let [name (.getClasspathResourceName this)
          source (ClasspathResourceSource/from name)]
      (when (or (string/ends-with? name ".clj")
                (string/ends-with? name ".cljc"))
        (select-new-vars source (fn [] (load name))))))
  ClassSelector
  (-select [this]
    (let [name (.getClassName this)
          ns-name (-> clazz-name
                      (string/replace "__init" "")
                      (string/replace "_" "-"))
          ns-sym (symbol ns-name)
          source (ClassSource/from name)]
      (when (string/ends-with? name "__init")
        (select-new-vars source (fn [] (require ns-sym))))))
  ClasspathRootSelector
  (-select [this]
    ;; TODO implement
    nil))

(defn select [^EngineDiscoveryRequest request ^UniqueId id]
  (binding [*all-vars* (atom (all-vars))]
    (let [listener (.getDiscoveryListener request)
          selectors (.getSelectorsByType request DiscoverySelector)]
      (loop [result []
             head (first selectors)
             tail (rest selectors)]
        (try
          (let [candidates (-select head)]
            (if candidates
              (.selectorProcessed listener id head (SelectorResolutionResult/resolved))
              (.selectorProcessed listener id head (SelectorResolutionResult/unresolved)))
            (if tail
              (recur (concat result candidates) (first tail) (rest tail))
              (concat result candidates)))
          (catch Exception e
            (.selectorProcessed listener id head (SelectorResolutionResult/failed e))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Discovery Descriptor Support
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- var->descriptor [{:keys [var]}]
  (ClojureVarDescriptor. (lang/->id var) var))

(defn- ns->descriptor [[ns candidates]]
  (let [ns-desc (ClojureNamespaceDescriptor. (lang/->id ns) ns)]
    (doseq [var-desc (map var->descriptor candidates)]
      (.addChild ns-desc var-desc))
    ns-desc))

(defn- do-discover [root-id candidates]
  (binding [lang/*root-id* root-id]
    (let [engine-desc (EngineDescriptor. root-id ClojureTestEngine/ENGINE_ID)
          ns-descs (->> candidates
                        (filter test?)
                        (group-by :namespace)
                        (map ns->descriptor))]
      (doseq [ns-desc ns-descs]
        (.addChild engine-desc ns-desc))
      engine-desc)))

(defn selections->descriptor [engine ^EngineDiscoveryRequest request ^UniqueId id candidates]
  (let [root-descriptor (EngineDescriptor. id (id engine))]))
;; TODO continue from here
