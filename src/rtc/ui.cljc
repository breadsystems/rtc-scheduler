(ns rtc.ui
  (:require
    [rum.core :as rum :exclude [cljsjs/react cljsjs/react-dom]]))

(defn wrap-rum-html [f]
  (fn [req]
    (let [res (f req)]
      (update res :body rum/render-static-markup))))

(defn Page [& {:keys [title content status head footer]
               :or {status 200}}]
  {:body
   [:html
    [:head
     [:meta {:charset :utf-8}]
     [:meta {:name :viewport :content "widht=device-width, initial-scale=1"}]
     #_ ;; TODO
     [:link {:rel :stylesheet
             :href "/css/style.css"}]
     [:title title " | Rad Telehealth Collective"]
     head]
    [:body
     [:main
      content]
     footer]]})
