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

(defn yes-or-no [b]
  (if b "yes" "no"))

(comment
  (filters->query-string {:a :b :c :d}))

(defn Page [& {:keys [title banner container-class content status head footer]
               :or {status 200}}]
  {:status status
   :body
   [:html #_{:data-color-mode :high-contrast}
    [:head
     [:meta {:charset :utf-8}]
     [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
     [:title title " | Rad Telehealth Collective"]
     [:link {:rel :stylesheet
             :href "/admin/admin.css"}]
     head]
    [:body
     banner
     [:nav
      [:a {:href "/admin"} [:h1 "RTC"]]
      [:ul
       [:li [:a {:href "/admin/appointments"} "Appointments"]]
       #_
       [:li [:a {:href "/admin/schedulers"} "Schedulers"]]
       #_
       [:li [:a {:href "/admin/providers"} "Providers"]]
       [:li [:a {:href "/account"} "My account"]]]]
     [:.container {:class container-class}
      content]
     [:footer footer]]]})

(defn NotFoundPage [_req]
  (Page
    :title "404"
    :content
    [:main
     [:h1 "404 Not found"]
     [:p "Sorry, we couldn't find the page you were looking for."]]))
