(ns rtc.validators-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.validators.core :as validators]))


(deftest test-validate-passwords
  (is (= {:password [{:message "Please enter a password"}]
          :password-confirmation []}
         (validators/validate-passwords {:password ""
                                         :password-confirmation "anything"})))
  (is (= {:password []
          :password-confirmation [{:message "Passwords do not match"}]}
         (validators/validate-passwords {:password "something"
                                         :password-confirmation "else"})))
  (is (= {:password [{:message "Please choose a longer password"}]
          :password-confirmation []}
         (validators/validate-passwords {:password "1234567"})))
  (is (true? (validators/validate-passwords {:password "longpassword"
                                             :password-confirmation "longpassword"}))))
