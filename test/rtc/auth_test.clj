(ns rtc.auth-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.auth :as auth]))


;; When a user hits /privileged/route without having logged in first,
;; we want to redirect them through the 2FA login flow and redirect
;; them to their initial destination once they've completed it.
(deftest test-login-uri
  ;; unauthenticated: /login with redirect query param
  (is (= "/login?next=/privileged/route"
         (auth/login-uri {:uri "/privileged/route"
                          :identity nil})))
  ;; Unauthenticated request to login screen, with prior redirect:
  ;; keep the prior redirect.
  (is (= "/login?next=/privileged/route"
         (auth/login-uri {:uri "/login"
                          :query-params {"next" "/privileged/route"}
                          :identity nil})))
  ;; Authenticated but unverified: keep prior redirect.
  (is (= "/login?next=/privileged/route"
         (auth/login-uri {:uri "/login"
                          :query-params {"next" "/privileged/route"}
                          :identity {:id 123}
                          :verified-2fa-token? false})))
  ;; Authenticated and verified request to the login screen, without
  ;; prior redirect, for some reason: default to volunteer screen.
  (is (= "/admin/volunteer"
         (auth/login-uri {:uri "/login"
                          :identity {:id 123}
                          :session {:verified-2fa-token? true}})))
  ;; Authenticated and verified!
  ;; Point the user to their original destination.
  (is (= "/privileged/route"
         (auth/login-uri {:uri "/login"
                          :query-params {"next" "/privileged/route"}
                          :identity {:id 123}
                          :session {:verified-2fa-token? true}}))))

(deftest test-login-step
  (is (= :unauthenticated (auth/login-step {:session nil})))
  (is (= :authenticating  (auth/login-step {:session nil
                                            :form-params {"email"    "any truthy value"
                                                          "password" "any truthy value"}})))
  (is (= :two-factor      (auth/login-step {:identity {:id 123}})))
  (is (= :verifying       (auth/login-step {:identity {:id 123}
                                            :form-params {"token" "12345678"}})))
  (is (= :logged-in       (auth/login-step {:identity {:id 123}
                                            :session {:verified-2fa-token? true}}))))

(deftest test-logout-handler
  (is (= {:status 302
          :headers {"Location" "/login"}
          :session {}
          :body ""}
         (auth/logout-handler {:uri "/logout"
                               :session {:identity {:id 123}}}))))