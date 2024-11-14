(ns rtc.admin
  (:require
    [rtc.ui :as ui]))

(defn show-providers [_]
  {:body "show providers"
   :status 200})

(defn show [_]
  (ui/Page
    :title "Admin"
    :footer
    [:<> [:script {:src "/js/admin.js"}]]
    :content
    [:p "It's the admin"]))
