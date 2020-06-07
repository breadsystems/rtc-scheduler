(ns rtc.util-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.util :as util]))


(deftest ->opt-works-polymorphically
  (is (= {:value :VALUE :label "LABEL"}
         (util/->opt {:value :VALUE :label "LABEL"})))
  (is (= {:value "JUST A STRING" :label "JUST A STRING"}
         (util/->opt "JUST A STRING"))))