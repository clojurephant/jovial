(ns org.ajoberstar.jupiter.engine.clojure-test.discovery
  (:require [clojure.test :as test]
            [clojure.tools.namespace.find :refer [find-namespaces]]))

(def ^:dynamic *tests* nil)

(defmulti scan-report :type)

(defmethod scan-report :begin-test-var [m]
  (let [test-var (-> m :var meta :old-var)]
    (swap! *tests* conj (meta test-var))))

(defmethod scan-report :default [_] nil)

(defn suppress-test-var
  [real-test-var]
  (fn [v]
    (let [old-meta (meta v)
          new-meta (assoc old-meta :test (fn [& _] nil) :old-var v)
          suppressed (with-meta @v new-meta)]
      (real-test-var suppressed))))

(defn scan-namespaces
  [namespaces]
  (let [real-test-var test/test-var]
    (binding [*tests* (atom [])
              test/report scan-report
              test/test-var (suppress-test-var real-test-var)]
      (apply test/run-tests namespaces)
      @*tests*)))

(defn scan-dirs
  [dirs]
  (let [namespaces (find-namespaces dirs)]
    ;; make sure each namespace is loaded
    (doseq [namespace namespaces]
      (require namespace))
    (scan-namespaces namespaces)))
