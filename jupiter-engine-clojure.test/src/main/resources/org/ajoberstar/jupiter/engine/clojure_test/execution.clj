(ns org.ajoberstar.jupiter.engine.clojure-test.execution
  (:require [clojure.test :as test]
            [clojure.string :as str])
  (:import (org.junit.gen5.engine TestExecutionResult EngineExecutionListener)
           (org.ajoberstar.jupiter.engine.clojure_test ClojureVarTestDescriptor)
           (org.junit.gen5.engine.support.descriptor EngineDescriptor)))

(def ^:dynamic *listener* nil)

(def ^:dynamic *root-descriptor* nil)

(def ^:dynamic *current-desc* nil)

(defn descriptor [var]
  (let [{:keys [ns name]} (meta var)
        unique-id (str ns "/" name)
        matches (fn [desc]
                  (if (= unique-id (.getUniqueId desc))
                    desc))]
    (some matches (.allDescendants *root-descriptor*))))

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
  (let [desc (descriptor (:var m))]
    (reset! *current-desc* desc)
    (.executionStarted *listener* desc)))

(defmethod listener-report :end-test-var [m]
  (let [desc (descriptor (:var m))]
    (reset! *current-desc* nil)))

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

(defn execute-tests [^EngineDescriptor descriptor ^EngineExecutionListener listener]
  (binding [*root-descriptor* descriptor
            *listener* listener
            *current-desc* (atom nil)
            test/report listener-report]
    (let [xf (comp (filter #(instance? ClojureVarTestDescriptor %))
                   (map (fn [desc]
                          (let [[namespace name] (str/split (.getName desc) #"/")]
                            (symbol namespace name))))
                   (map find-var))
          vars (sequence xf (.allDescendants *root-descriptor*))]
      (println vars)
      (test/test-vars vars))))
