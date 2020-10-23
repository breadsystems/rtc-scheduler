(ns rtc.two-factor-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.auth.two-factor :as two-factor]))


(deftest test-verified
  (is (two-factor/verified? {:session {:verified-2fa-token? true}}))
  (is (not (two-factor/verified? {:session {:verified-2fa-token? false}})))
  (is (not (two-factor/verified? {:session {}})))
  (is (not (two-factor/verified? {}))))

(deftest test-require-verification
  ;; 2FA disabled (empty preference value)
  (is (false? (two-factor/require-verification?
               {:identity {:preferences {}}})))
  ;; 2FA disabled
  (is (false? (two-factor/require-verification?
               {:identity {:preferences {:two-factor-enabled? false}}})))
  ;; 2FA enabled and has already verified
  (is (false? (two-factor/require-verification?
               {:identity {:preferences {:two-factor-enabled? true}}
                :session {:verified-2fa-token? true}})))
  (is (true? (two-factor/require-verification?
              {:identity {:preferences {:two-factor-enabled? true}}}))))