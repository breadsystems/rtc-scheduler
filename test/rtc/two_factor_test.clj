(ns rtc.two-factor-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.auth.two-factor :as two-factor]))


(deftest test-verified
  (is (two-factor/verified? {:session {:verified-2fa-token? true}}))
  (is (not (two-factor/verified? {:session {:verified-2fa-token? false}})))
  (is (not (two-factor/verified? {:session {}})))
  (is (not (two-factor/verified? {}))))