(ns sample.core-test
  (:require [clojure.test :refer :all]))

(deftest my-sample-works
  (is (= 1 (- 2 1))))

(deftest my-sample-fails
  (is (= 5 (+ 2 2))))

(deftest multiple-fails
  (testing "this is a set of stuff"
    (is (= 4 (* 2 2)))
    (is (= 5 (* 2 2))))
  (testing "second set of stuff"
    (is (= 6 (* 2 4)) "This isn't true")))
