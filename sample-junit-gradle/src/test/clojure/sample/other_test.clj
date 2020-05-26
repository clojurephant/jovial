(ns sample.other-test
    (:require [clojure.test :refer :all]))

(deftest my-other-works
  (is (= 1 (- 2 1)) "2 - 1 is 1"))

(deftest my-other-fails
  (is (= 5 (+ 2 2)) "2 + 2 is 5"))

(defn do-stuff [_]
  (throw (ex-info "Yay it works!" {})))

(deftest my-other-error
  (is (= 1 (do-stuff 7)) "Do stuff always returns 1"))
