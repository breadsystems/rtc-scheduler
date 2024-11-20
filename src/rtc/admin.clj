(ns rtc.admin
  (:require
    [rtc.ui :as ui]))

(defn- coerce-filter-params [params coercions]
  (reduce (fn [filters [k coerce]]
            (update filters k coerce))
          params coercions))

(defn wrap-filter-params [{:keys [query]}]
  (fn [f]
    (fn [req]
      (let [filters (coerce-filter-params (:params req) query)]
        (f (assoc req :filters filters))))))

(defn show-providers [_]
  {:body "TODO show providers"
   :status 200})

(defn show [_]
  (ui/Page
    :title "Admin"
    :footer
    ;; TODO code splitting
    [:<> [:script {:src "/js/admin.js"}]]
    :content
    [:main.spacious
     [:article.card
      [:a {:href "/admin/appointments"}
       [:h1 "Appointments"]]
      [:a {:href "/admin/appointments?status=needs-attention"}
       [:h2 3 #_(count $appointments) " Appointments need follow-up"]]
      [:a {:href "/admin/appointments?status=scheduled"}
       [:h2 4 #_(count $appointments) " scheduled"]]
      [:a {:href "/admin/appointments?status=archived"}
       [:h2 35 #_(count $appointments) " archived"]]]
     #_
     [:article.card
      [:h1 (count []) " providers"]
      [:div.instruct "Provider roster coming soon"]]]))
