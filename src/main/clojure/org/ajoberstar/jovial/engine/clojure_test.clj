(ns org.ajoberstar.jovial.engine.clojure-test
  (:refer-clojure :exclude [descriptor])
  (:require [clojure.test :as test]
            [org.ajoberstar.jovial.engine :as engine])
  (:import [org.ajoberstar.jovial ClojureTestEngine ClojureNamespaceDescriptor ClojureVarDescriptor]
           [org.opentest4j AssertionFailedError]
           [org.junit.platform.engine EngineExecutionListener TestDescriptor TestExecutionResult ConfigurationParameters]
           [org.junit.platform.engine.support.descriptor EngineDescriptor]))

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
    (let [^Throwable root (first errs)]
      (doseq [sup (rest errs)]
        (.addSuppressed root sup))
      (TestExecutionResult/failed root))
    (TestExecutionResult/successful)))

(defn execute-node [^TestDescriptor descriptor ^EngineExecutionListener listener]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reporter
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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
(defn- test? [cand]
  (-> cand :sym requiring-resolve meta :test))

(defrecord Engine [config]
  engine/Engine
  (id [_] ClojureTestEngine/ENGINE_ID)
  (discover [_ request id]
    (let [candidates (engine/select request)
          selected (filter test? candidates)]
      (engine/selected->descriptor request id selected)))
  (execute [_ descriptor listener]
    (binding [test/report jovial-report]
      (execute-node descriptor listener))))

(defn engine [^ConfigurationParameters config]
  (->Engine config))
