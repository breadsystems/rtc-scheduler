(ns rtc.i18n-data-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.i18n.data :as data]))


(deftest test-reduce-lang-maps
  (let [english {:lang :en :yes "Yes" :no "No"}
        español {:lang :es :yes "Sí"  :no "No"}]
    (is (= {:en english :es español}
           (data/reduce-langs [english español])))))