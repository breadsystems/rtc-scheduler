(ns rtc.users.handlers
  (:require
   [rtc.assets.core :as assets]
   [rtc.layout :as layout]
   [rtc.users.core :as u]))


(defn- register-page [req]
  (layout/page
   {:title "Register"
    :head
    [[:link {:rel :stylesheet :href (assets/style-href :admin)}]]
    :content
    [:div#rtc-registration]
    :footer-content
    [:div
     [:meta {:name "email" :content (get-in req [:query-params "email"])}]
     [:meta {:name "code"  :content (get-in req [:query-params "code"])}]
     [:script {:src "/js/shared.js" :type "text/javascript"}]
     [:script {:src "/js/register.js" :type "text/javascript"}]]}))

(defn- reset-pass-page [req]
  (layout/page
   {:title "Reset Password"
    :head
    [[:link {:rel :stylesheet :href (assets/style-href :admin)}]]
    :content
    [:div#rtc-registration]
    :footer-content
    [:div
     [:meta {:name "email" :content (get-in req [:query-params "email"])}]
     [:meta {:name "code"  :content (get-in req [:query-params "code"])}]
     [:script {:src "/js/shared.js" :type "text/javascript"}]
     [:script {:src "/js/reset-pass.js" :type "text/javascript"}]]}))

(defn register-handler [{:keys [params] :as req}]
  (if (u/validate-invitation {:email (:email params) :code (:code params)})
    (register-page req)
    {:status 401
     :headers {"Content-Type" "text/plain"}
     :body "Invalid email or invitation code"}))

(defn reset-pass-handler [{:keys [params] :as req}]
  (if (u/validate-invitation {:email (:email params) :code (:code params)})
    (reset-pass-page req)
    {:status 401
     :headers {"Content-Type" "text/plain"}
     :body "Invalid email or invitation code"}))
