(ns sample.core-test
  {:integration true}
  (:require [clojure.test :refer :all]))

(deftest my-sample-works
  (is (= 1 (- 2 1))))

(deftest ^{:integration false} my-sample-fails
  (is (= 5 (+ 2 2))))
