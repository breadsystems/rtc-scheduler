(ns rtc.layout
  (:require
   [hiccup.core :refer [html]]
   [hiccup.page :refer [doctype]]))


(defn page [opts]
  (let [{:keys [title head content footer-content]} opts]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body
     (html
      (doctype :html5)
      [:html
       (conj [:head
              [:title (str title " | Radical Telehealth Collective")]
              [:meta {:charset "utf-8"}]
              [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]]
             head)
       (conj [:body
              [:header "site header"]
              content
              [:footer "site footer"]]
             footer-content)])}))


(defn login-page [{:keys [form-params]}]
  (page
   {:title "Login"
    :content
    [:main "Login main"]}))