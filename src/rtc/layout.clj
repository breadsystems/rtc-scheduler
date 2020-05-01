(ns rtc.layout
  (:require
   [buddy.auth :refer [authenticated?]]
   [hiccup.core :refer [html]]
   [hiccup.page :refer [doctype]]))


(defn error-page [opts]
  (let [{:keys [err req]} opts]
    {:status 400
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body
     (html
      (doctype :html5)
      [:html
       [:head
        [:title (str "ERROR: " err)]
        [:meta {:charset "utf-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:meta {:name "csrf-token" :content (:anti-forgery-token req)}]]
       [:body
        [:pre (str req)]]])}))


(defn page [opts]
  (let [{:keys [title head content footer-content req]} opts]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body
     (html
      (doctype :html5)
      [:html
       (conj [:head
              [:title (str title " | Radical Telehealth Collective")]
              [:meta {:charset "utf-8"}]
              [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
              [:meta {:name "csrf-token" :content (:anti-forgery-token req)}]]
             head)
       (conj [:body
              [:header "site header"
               (when (authenticated? req)
                 [:a.logout {:href "/logout"} "Logout"])]
              content
              [:footer "site footer"]]
             footer-content)])}))


(defn login-page [{:keys [form-params uri query-string] :as req}]
  (let [{:keys [email password]} form-params]
    (page
     {:title "Login"
      :req req
      :content
      [:main
       [:form {:action (str uri "?" query-string) :method "POST"}
        [:input {:type :email
                 :name "email"
                 ;; TODO
                 :value (or email "rtc-admin@example.com")
                 :placeholder "me@example.com"}]
        [:input {:type :password
                 :name "password"
                 ;; TODO
                 :value (or password "bgf7ekabllojGyvZ")}]
        [:button {:type :submit} "Login"]]]})))