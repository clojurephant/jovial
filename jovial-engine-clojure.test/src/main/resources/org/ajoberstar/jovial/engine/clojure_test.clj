(ns org.ajoberstar.jovial.engine.clojure-test
  (:require [org.ajoberstar.jovial.lang.clojure :as lang]
            [org.ajoberstar.jovial.lang.clojure.engine :as engine]
            [clojure.test :as test]
            [clojure.stacktrace :as stack])
  (:import (org.ajoberstar.jovial.engine.clojure_test ClojureTestEngine ClojureTestDescriptor)
           (org.opentest4j AssertionFailedError)
           (org.junit.platform.engine TestTag TestExecutionResult ConfigurationParameters)
           (org.junit.platform.engine.support.descriptor EngineDescriptor)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Discover support
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- test? [cand]
  (-> cand :var meta :test))

(def ^:private excluded-tags #{:ns :file :line :column :doc :author :test :name})

(defn- tags [var]
  (let [var-meta (-> var meta)
        ns-meta (-> var-meta :ns meta)
        full-meta (merge ns-meta var-meta)
        xf (comp (filter second)
                 (map first)
                 (remove excluded-tags)
                 (map name)
                 (map #(TestTag/create %)))]
    (into #{} xf full-meta)))

(defn- var->descriptor [{:keys [var source]}]
  (ClojureTestDescriptor. (lang/->id var) (lang/->friendly var) (tags var) source))

(defn- ns->descriptor [[ns candidates]]
  (let [ns-desc (ClojureTestDescriptor. (lang/->id ns) (lang/->friendly ns) (tags ns) (lang/ns-source ns))]
    (doseq [var-desc (map var->descriptor candidates)]
      (.addChild ns-desc var-desc))
    ns-desc))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Execute support
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *listener* nil)

(def ^:dynamic *descs* nil)

(def ^:dynamic *current-desc* nil)

(defn descriptors [root]
  (let [xf (map (fn [desc] [(-> desc .getSource lang/->ref) desc]))]
    (into {} xf (.getAllDescendants root))))

(defmulti listener-report :type)

(defmethod listener-report :begin-test-var [m]
  (let [desc (->> m :var (get *descs*))]
    (reset! *current-desc* desc)
    (.executionStarted *listener* desc)))

(defmethod listener-report :end-test-var [m]
  (reset! *current-desc* nil))

(defmethod listener-report :pass [m]
  (let [desc @*current-desc*
        result (TestExecutionResult/successful)]
    (.executionFinished *listener* desc result)))

(defn- ex-fail [{:keys [message expected actual] :as m}]
  (let [msg (with-out-str
              (println "FAIL in " (test/testing-vars-str m))
              (when (seq test/*testing-contexts*)
                (println (test/testing-contexts-str)))
              (when message
                (println message))
              (println "expected: " expected)
              (println "  actual: " actual))]
    (AssertionFailedError. msg expected actual)))

(defmethod listener-report :fail [m]
  (let [desc @*current-desc*
        result (TestExecutionResult/failed (ex-fail m))]
    (.executionFinished *listener* desc result)))

(defn- ex-error [{:keys [message expected actual] :as m}]
  (let [msg (with-out-str
              (println "ERROR in " (test/testing-vars-str m))
              (when (seq test/*testing-contexts*)
                (println (test/testing-contexts-str)))
              (when message
                (println message))
              (println "expected: " expected)
              (print "  actual: ")
              (stack/print-cause-trace actual test/*stack-trace-depth*))]
    (AssertionFailedError. msg actual)))

(defmethod listener-report :error [m]
  (let [desc @*current-desc*
        result (TestExecutionResult/failed (ex-error m))]
    (.executionFinished *listener* desc result)))

(defmethod listener-report :default [_] nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; High-level
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Engine []
  engine/Engine
  (-discover [_ root-id candidates]
    (binding [lang/*root-id* root-id]
      (let [engine-desc (EngineDescriptor. root-id ClojureTestEngine/ENGINE_ID)
            ns-descs (->> candidates
                          (filter test?)
                          (group-by :namespace)
                          (map ns->descriptor))]
        (doseq [ns-desc ns-descs]
          (.addChild engine-desc ns-desc))
        engine-desc)))
  (-execute [_ descriptor listener]
    (binding [*listener* listener
              *descs* (descriptors descriptor)
              *current-desc* (atom nil)
              test/report listener-report]
      (let [selected-vars (keys *descs*)]
        (test/test-vars selected-vars)))))

(defn engine [^ConfigurationParameters config]
  ;; could support config at some point
  (->Engine))
