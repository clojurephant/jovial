(ns org.ajoberstar.jovial.engine.clojure-test
  (:require [org.ajoberstar.jovial.lang.clojure :as lang]
            [org.ajoberstar.jovial.lang.clojure.engine :as engine]
            [clojure.test :as test]
            [clojure.stacktrace :as stack])
  (:import (org.ajoberstar.jovial.engine.clojure_test ClojureTestEngine ClojureNamespaceDescriptor ClojureVarDescriptor)
           (org.opentest4j AssertionFailedError)
           (org.junit.platform.engine TestTag TestDescriptor TestExecutionResult ConfigurationParameters)
           (org.junit.platform.engine.reporting ReportEntry)
           (org.junit.platform.engine.support.descriptor EngineDescriptor)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Discover support
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- test? [cand]
  (-> cand :var meta :test))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Execute support
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *throwable* nil)
(def ^:dynamic *entries* nil)

(defmulti jovial-report :type)

;; ignore these ones, since we're handling on our own
(defmethod jovial-report :begin-test-ns [m])
(defmethod jovial-report :end-test-ns [m])
(defmethod jovial-report :begin-test-var [m])
(defmethod jovial-report :end-test-var [m])

;; success doesn't need to be reported
(defmethod jovial-report :pass [m])

(defmethod jovial-report :fail [m]
  (let [{:keys [message expected actual]}  m
        msg (with-out-str
              (println "FAIL in " (test/testing-vars-str m))
              (when (seq test/*testing-contexts*)
                (println (test/testing-contexts-str)))
              (when message
                (println message))
              (println "expected: " expected)
              (println "  actual: " actual))]
    (reset! *throwable* (AssertionFailedError. msg expected actual))))

(defmethod jovial-report :error [m]
  (let [{:keys [message expected actual]} m
        msg (with-out-str
              (println "ERROR in " (test/testing-vars-str m))
              (when (seq test/*testing-contexts*)
                (println (test/testing-contexts-str)))
              (when message
                (println message))
              (println "expected: " expected)
              (print "  actual: ")
              (stack/print-cause-trace actual test/*stack-trace-depth*))]
    (reset! *throwable* (AssertionFailedError. msg actual))))

(defmethod jovial-report :default [m]
  (swap! *entries* conj (ReportEntry/from (dissoc m :type))))

(declare try-execute)

(defprotocol Fixture
  (-fixture [desc]))

(extend-protocol Fixture
  Object
  (-fixture [desc]
    (fn [f] (f)))
  ClojureVarDescriptor
  (-fixture [desc]
    (-> desc .getNamespace meta ::test/each-fixtures test/join-fixtures))
  ClojureNamespaceDescriptor
  (-fixture [desc]
    (-> desc .getNamespace meta ::test/once-fixtures test/join-fixtures)))

(defprotocol Test
  (-test [desc listener]))

(extend-protocol Test
  TestDescriptor
  (-test [desc listener]
    (fn []
      (doseq [child (.getChildren desc)]
        (try-execute child listener))))
  ClojureVarDescriptor
  (-test [desc _]
    (let [test (-> desc .getVar meta :test)]
      (fn []
        (binding [test/*testing-vars* (conj test/*testing-vars* (.getVar desc))]
          (test))))))

(defn try-execute [descriptor listener]
  (binding [*throwable* (atom nil)
            *entries* (atom [])
            test/report jovial-report]
    (try
      (.executionStarted listener descriptor)
      (let [fixture (-fixture descriptor)
            test (-test descriptor listener)]
        (fixture
          (fn []
            (test))))
      (catch Exception e
        (reset! *throwable* e)))
    (doseq [entry @*entries*]
      (.reportingEntryPublished listener descriptor entry))
    (let [e @*throwable*
          result (if e (TestExecutionResult/failed e) (TestExecutionResult/successful))]
      (.executionFinished listener descriptor result))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; High-level
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Engine []
  engine/Engine
  (-discover [_ root-id candidates]
    (do-discover root-id candidates))
  (-execute [_ descriptor listener]
    (try-execute descriptor listener)))

(defn engine [^ConfigurationParameters config]
  ;; could support config at some point
  (->Engine))
