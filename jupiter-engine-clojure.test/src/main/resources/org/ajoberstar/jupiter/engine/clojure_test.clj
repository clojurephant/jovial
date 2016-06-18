(ns org.ajoberstar.jupiter.engine.clojure-test
  (:require [org.ajoberstar.jupiter.lang.clojure :as lang]
            [org.ajoberstar.jupiter.lang.clojure.engine :as engine]
            [clojure.test :as test])
  (:import (org.junit.gen5.engine ConfigurationParameters TestExecutionResult)
           (org.junit.gen5.engine.support.descriptor EngineDescriptor)
           (org.ajoberstar.jupiter.engine.clojure_test ClojureTestEngine ClojureTestDescriptor)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Discover support
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- test? [cand]
  (-> cand :var meta :test))

(defn- var->descriptor [{:keys [var source]}]
  (ClojureTestDescriptor. (lang/->id var) (lang/->friendly var) source))

(defn- ns->descriptor [[ns candidates]]
  (let [ns-desc (ClojureTestDescriptor. (lang/->id ns) (lang/->friendly ns) (lang/ns-source ns))]
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
