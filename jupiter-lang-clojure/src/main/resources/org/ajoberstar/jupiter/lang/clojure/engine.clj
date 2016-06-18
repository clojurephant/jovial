(ns org.ajoberstar.jupiter.lang.clojure.engine
  (:require [org.ajoberstar.jupiter.lang.clojure.impl.discovery :as discovery])
  (:import (org.junit.gen5.engine EngineDiscoveryRequest UniqueId ExecutionRequest)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Specification of an Engine
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol Engine
  (-discover [engine root-id candidates] "Discovers tests among the candidates, should return a test descriptor.")
  (-execute [engine descriptor listener] "Executes the tests in the descriptor and reports results to the listener."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; High-level interface
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn discover [engine ^EngineDiscoveryRequest request ^UniqueId root-id]
  (->> (discovery/select request)
       (discovery/filter request)
       (-discover engine root-id)))

(defn execute [engine ^ExecutionRequest request]
  (-execute engine (.getRootTestDescriptor request) (.getEngineExecutionListener request)))
