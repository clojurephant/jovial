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
(def ^:dynamic *throwables* nil)

(defprotocol TestNode
  (-fixture [descriptor])
  (-execute [descriptor]))

(extend-protocol TestNode
  TestDescriptor
  (-fixture [_] (fn [f] (f)))
  (-execute [_] nil)

  ClojureNamespaceDescriptor
  (-fixture [descriptor] (-> (.getNamespace descriptor) meta ::test/once-fixtures test/join-fixtures))
  (-execute [_] nil)

  ClojureVarDescriptor
  (-fixture [descriptor] (-> (.getNamespace descriptor) meta ::test/each-fixtures test/join-fixtures))
  (-execute [descriptor]
    (binding [test/*testing-vars* (conj test/*testing-vars* (.getVar descriptor))]
      (try
        ((-> descriptor .getVar meta :test))
        (catch Throwable e
          (test/do-report {:type :error :message "Uncaught exception, not in assertion." :expected nil :actual e}))))))

(defn- result [errs]
  (if (seq errs)
    (let [root (first errs)]
      (doseq [sup (rest errs)]
        (.addSuppressed root sup))
      (TestExecutionResult/failed root))
    (TestExecutionResult/successful)))

(defn execute-node [descriptor listener]
  (binding [*throwables* (atom [])]
    (.executionStarted listener descriptor)
    (let [fixture (-fixture descriptor)]
      (try
        (fixture
          (fn []
            (-execute descriptor)
            (doseq [child (.getChildren descriptor)]
              (execute-node child listener))))
        (catch Throwable e
          (test/do-report {:type :error :message "Uncaught exception, in fixtures." :expected nil :actual e}))))
    (.executionFinished listener descriptor (result @*throwables*))))

(defmulti jovial-report :type)

(defmethod jovial-report :fail [m]
  (let [{:keys [message expected actual]}  m
        msg (with-out-str
              (println "FAIL in " (test/testing-vars-str m))
              (when (seq test/*testing-contexts*)
                (println (test/testing-contexts-str)))
              (when message
                (println message))
              (println "expected: " (pr-str expected))
              (println "  actual: " (pr-str actual)))]
    (swap! *throwables* conj (AssertionFailedError. msg expected actual))))

(defmethod jovial-report :error [m]
  (let [{:keys [message expected actual]}  m
        msg (with-out-str
              (println "ERROR in " (test/testing-vars-str m))
              (when (seq test/*testing-contexts*)
                (println (test/testing-contexts-str)))
              (when message
                (println message))
              (println "expected: " (pr-str expected)))]
    (swap! *throwables* conj (AssertionFailedError. msg actual))))

;; ignore these ones, we're managing this directly
(defmethod jovial-report :begin-test-ns [m])
(defmethod jovial-report :end-test-ns [m])
(defmethod jovial-report :begin-test-var [m])
(defmethod jovial-report :end-test-var [m])
(defmethod jovial-report :pass [m])
(defmethod jovial-report :default [m])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; High-level
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Engine []
  engine/Engine
  (-discover [_ root-id candidates]
    (do-discover root-id candidates))
  (-execute [_ descriptor listener]
    (binding [test/report jovial-report]
      (execute-node descriptor listener))))

(defn engine [^ConfigurationParameters config]
  ;; could support config at some point
  (->Engine))
