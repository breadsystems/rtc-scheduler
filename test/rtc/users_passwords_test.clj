(ns rtc.users-passwords-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.users.passwords :as pass]))


(deftest test-validate-passwords
  (is (= {:password [{:message "Please enter a password"}]
          :password-confirmation []}
         (pass/validate-passwords {:password ""
                                         :password-confirmation "anything"})))
  (is (= {:password []
          :password-confirmation [{:message "Passwords do not match"}]}
         (pass/validate-passwords {:password "something"
                                         :password-confirmation "else"})))
  (is (= {:password [{:message "Please choose a longer password"}]
          :password-confirmation []}
         (pass/validate-passwords {:password "1234567"})))
  (is (true? (pass/validate-passwords {:password "longpassword"
                                             :password-confirmation "longpassword"}))))
