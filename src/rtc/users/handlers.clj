(ns rtc.users.handlers
  (:require
   [rtc.layout :as layout]
   [rtc.users.core :as u]))


(defn- register-form [req]
  (layout/page
   {:title "Register"
    :content
    [:div#rtc-registration]
    :footer-content
    [:div
     [:meta {:name "email" :content (get-in req [:query-params "email"])}]
     [:meta {:name "code"  :content (get-in req [:query-params "code"])}]
     [:script {:src "/js/shared.js" :type "text/javascript"}]
     [:script {:src "/js/register.js" :type "text/javascript"}]]}))

(defn register-resolver [_context args _value]
  (fn [_context args _value]
    (let [{:keys [email
                  invite_code
                  first_name
                  last_name
                  phone
                  password
                  password_confirmation]} args]
      (if-not (u/validate-invitation args)
        (throw (ex-info "Invalid email or invitation code" {}))
        nil))))


(defn register-handler [{:keys [query-params] :as req}]
  (let [email (get query-params "email")
        code  (get query-params "code")]
    (if (u/validate-invitation {:email email :code code})
      (register-form req)
      {:status 401
       :headers {"Content-Type" "text/plain"}
       :body "Invalid email or invitation code"})))