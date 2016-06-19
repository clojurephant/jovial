(ns sample.core-test
  (:require [clojure.test :refer :all])
  (:gen-class))

(deftest my-sample-works
  (is (= 1 (- 2 1))))

(deftest my-sample-fails
  (is (= 5 (+ 2 2))))
