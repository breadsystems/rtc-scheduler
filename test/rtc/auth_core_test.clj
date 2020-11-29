(ns rtc.auth-core-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.auth.core :as auth]))


(deftest test-login-step
  (is (= :unauthenticated (auth/login-step {:session nil})))
  (is (= :authenticating  (auth/login-step {:session nil
                                            :params {:email "any truthy value"
                                                     :password "any truthy value"}})))
  (is (= :two-factor      (auth/login-step {:identity {:id 123}})))
  (is (= :verifying       (auth/login-step {:identity {:id 123}
                                            :params {:token "12345678"}})))
  (is (= :logged-in       (auth/login-step {:identity {:id 123}
                                            :session {:verified-2fa-token? true}}))))

(deftest test-logout-handler
  (is (= {:status 302
          :headers {"Location" "/login"}
          :session {}
          :body ""}
         (auth/logout-handler {:uri "/logout"
                               :session {:identity {:id 123}}}))))