(ns sample.other-test
    (:require [clojure.test :refer :all]))

(deftest my-other-works
  (is (= 1 (- 2 1))))

(deftest my-other-fails
  (is (= 5 (+ 2 2))))

(deftest my-other-error
  (throw (ex-info "Yay it works!" {})))
