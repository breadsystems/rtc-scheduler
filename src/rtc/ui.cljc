(ns rtc.ui
  (:require
    [rum.core :as rum :exclude [cljsjs/react cljsjs/react-dom]])
  (:import
    [java.text SimpleDateFormat]))

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

(def fmt-ymd (SimpleDateFormat. "yyyy-MM-dd HH:mm a z"))

(defn DebugFooter [{:keys [clojure-version
                           git-hash
                           release-version
                           started-at]}]
  [:.bottom
   (str "Release version: " git-hash
        " | Started at: " (when started-at
                            (.format fmt-ymd started-at))
        " | Clojure version: " clojure-version)])

(defn Page [& {:keys [title container-class content status head footer system]
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
     [:footer
      footer
      (DebugFooter system)]]]})

(defn NotFoundPage [{:keys [system]}]
  (Page
    :system system
    :title "404"
    :content
    [:main
     [:h1 "404 Not found"]
     [:p "Sorry, we couldn't find the page you were looking for."]]))
