(ns rtc.ui
  (:require
    [rum.core :as rum :exclude [cljsjs/react cljsjs/react-dom]]))

(defn wrap-rum-html [f]
  (fn [req]
    (let [res (f req)]
      (update res :body rum/render-static-markup))))

(defn Option [labels selected-value value]
  [:option {:value value
            :label (labels value)
            :selected (= selected-value value)}])

(defn- filter-pair [[k v]]
  (str (name k) "=" (when v (name v))))

(defn filters->query-string [filters]
  (str "?" (clojure.string/join "&" (map filter-pair filters))))

(comment
  (filters->query-string {:a :b :c :d}))

(defn Page [& {:keys [title content status head footer]
               :or {status 200}}]
  {:body
   [:html #_{:data-color-mode :high-contrast}
    [:head
     [:meta {:charset :utf-8}]
     [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
     [:title title " | Rad Telehealth Collective"]
     [:link {:rel :stylesheet
             :href "/admin/admin.css"}]
     head]
    [:body
     [:nav
      [:h1 "RTC"]
      [:ul
       [:li [:a {:href "/admin/appointments"} "Appointments"]]
       [:li [:a {:href "/admin/schedulers"} "Schedulers"]]
       [:li [:a {:href "/admin/providers"} "Providers"]]
       [:li [:a {:href "/account"} "My account"]]]]
     [:.container
      content
      footer]]]})
