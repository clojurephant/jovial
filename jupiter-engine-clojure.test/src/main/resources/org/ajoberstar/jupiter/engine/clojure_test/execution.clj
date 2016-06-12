(ns org.ajoberstar.jupiter.engine.clojure-test.execution
  (:require [clojure.test :as test]
            [org.ajoberstar.jupiter.lang.clojure :as lang])
  (:import (org.junit.gen5.engine TestExecutionResult EngineExecutionListener)
           (org.junit.gen5.engine.support.descriptor EngineDescriptor)))

(def ^:dynamic *listener* nil)

(def ^:dynamic *descs* nil)

(def ^:dynamic *current-desc* nil)

(defn descriptors [root]
  (let [xf (map (fn [desc] [(-> desc .getSource lang/->ref) desc]))]
    (into {} xf (.getAllDescendants root))))

(defn ex-fail [{:keys [message expected actual]}]
  (let [msg (with-out-str
              (println (or message ""))
              (if expected
                (println "expected: " (pr-str expected)))
              (if-not (instance? Throwable actual)
                (println "actual:   " (pr-str actual))))]
    (if (instance? Throwable actual)
      (ex-info msg {} actual)
      (ex-info msg {}))))

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

(defmethod listener-report :fail [m]
  (let [desc @*current-desc*
        result (TestExecutionResult/failed (ex-fail m))]
    (.executionFinished *listener* desc result)))

(defmethod listener-report :error [m]
  (let [desc @*current-desc*
        result (TestExecutionResult/failed (ex-fail m))]
    (.executionFinished *listener* desc result)))

(defmethod listener-report :default [_] nil)

(defn filtering-test-var [real-test-var]
  (fn [v]
    (if (contains? *descs* v)
      (real-test-var v))))

(defn execute-tests [^EngineDescriptor descriptor ^EngineExecutionListener listener]
  (binding [*listener* listener
            *descs* (descriptors descriptor)
            *current-desc* (atom nil)
            test/test-var (filtering-test-var test/test-var)
            test/report listener-report]
    (test/run-all-tests)))
