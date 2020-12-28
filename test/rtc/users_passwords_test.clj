(ns rtc.users-passwords-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.users.passwords :as pass]))


(deftest test-validate-passwords
  (is (= {:pass [{:message "Please enter a password"}]
          :pass-confirmation []}
         (pass/validate-passwords {:pass ""
                                   :pass-confirmation "anything"})))
  (is (= {:pass []
          :pass-confirmation [{:message "Passwords do not match"}]}
         (pass/validate-passwords {:pass "something"
                                   :pass-confirmation "else"})))
  (is (= {:pass [{:message "Please choose a longer password"}]
          :pass-confirmation []}
         (pass/validate-passwords {:pass "1234567"})))
  (is (true? (pass/validate-passwords {:pass "longpassword"
                                       :pass-confirmation "longpassword"}))))
