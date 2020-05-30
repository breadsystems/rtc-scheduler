(ns rtc.auth-core-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.auth.core :as auth]))


(deftest test-login-step
  (is (= :unauthenticated (auth/login-step {:session nil})))
  (is (= :authenticating  (auth/login-step {:session nil
                                            :form-params {"email"    "any truthy value"
                                                          "password" "any truthy value"}})))
  (is (= :two-factor      (auth/login-step {:session {:identity {:id 123}}})))
  (is (= :verifying       (auth/login-step {:session {:identity {:id 123}}
                                            :form-params {"token" "12345678"}})))
  (is (= :logged-in       (auth/login-step {:session {:identity {:id 123}
                                                      :verified-2fa-token? true}}))))

(deftest test-logout-handler
  (is (= {:status 302
          :headers {"Location" "/login"}
          :session {}
          :body ""}
         (auth/logout-handler {:uri "/logout"
                               :session {:identity {:id 123}}}))))

(deftest test-admin-only-resolver
  (let [my-resolver (fn [_ _ _]
                      {:some :data})
        secure-resolver (auth/admin-only-resolver my-resolver)]
    (is (= {:errors [{:message "You do not have permission to do that"}]}
           (secure-resolver {:request {:session {:identity nil}}} "query string" nil)))
    (is (= {:some :data}
           (secure-resolver {:request {:session {:identity {:is_admin true}}}} "query string" nil)))))