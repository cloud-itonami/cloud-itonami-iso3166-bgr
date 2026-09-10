(ns statute.facts-test
  (:require [kotoba.lang.text :as str]
            [clojure.test :refer [deftest is]]
            [statute.facts :as facts]))

(deftest bgr-has-spec-basis
  (let [sb (facts/spec-basis "BGR")]
    (is (= 3 (count sb)))
    (is (every? #(str/starts-with? (:statute/url %) "https://") sb))
    (is (every? :statute/law-number sb))))

(deftest unknown-jurisdiction-has-no-spec-basis
  (is (nil? (facts/spec-basis "ATL")))
  (is (nil? (facts/spec-basis "ZZZ"))))

(deftest coverage-is-honest
  (let [c (facts/coverage ["BGR" "JPN" "ATL"])]
    (is (= 3 (:requested c)))
    (is (= 1 (:covered c)))
    (is (= ["ATL" "JPN"] (:missing-jurisdictions c)))))

(deftest by-topic-filters
  (is (= ["bgr.labour-code"]
         (mapv :statute/id (facts/by-topic "BGR" :labor))))
  (is (empty? (facts/by-topic "BGR" :environment)))
  (is (empty? (facts/by-topic "ATL" :labor))))
