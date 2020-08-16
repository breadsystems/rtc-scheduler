(ns rtc.style-build-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.style.build :as style]))


(deftest test-compile-garden-assets
  (let [assets [{:name :main
                 :opts {:pretty-print? false}
                 :styles [[:* {:color :red}]]}]]
    (is (= [{:name :main
             :hash "8d3a17931b113297914ad910669c5691"
             :file "main.8d3a17931b113297914ad910669c5691.css"
             :contents "*{color:red}"}]
           (style/compile-garden-assets assets)))))