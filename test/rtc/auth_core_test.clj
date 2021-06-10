(ns rtc.auth-core-test
  (:require
   [clojure.test :refer [are deftest is]]
   [rtc.auth.core :as auth]
   [rtc.users.core :as u]))


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

(deftest test-wrap-identity-middleware
  (let [handler identity
        wrapped (auth/wrap-identity handler)
        users {123 {:info "extra stuf" :is_admin false :is_provider nil}
               456 {:info "extra info" :is_admin true :is_provider nil}
               789 {:info "extra blah" :is_admin true :is_provider true}}]
    (with-redefs [u/id->user users]
      (are [user req] (= user (-> req wrapped :identity))

           nil
           {:uri "/"}

           nil
           {:uri "/" :session nil}

           nil
           {:uri "/" :session {}}

           {:id 123 :authy_id 456 :admin? false :provider? false}
           {:uri "/" :session {:identity {:id 123 :authy_id 456}}}

           {:id 456 :authy_id 456 :admin? true :provider? false}
           {:uri "/"
            :session {:identity {:id 456 :authy_id 456}}}

           {:id 789 :authy_id 456 :admin? true :provider? true}
           {:uri "/"
            :session {:identity {:id 789 :authy_id 456}}}))))
