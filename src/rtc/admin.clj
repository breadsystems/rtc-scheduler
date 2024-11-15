(ns rtc.admin
  (:require
    [rtc.ui :as ui]))

(defn- coerce-filter-params [params coercions]
  (reduce (fn [filters [k coerce]]
            (update filters k coerce))
          params coercions))

(defn wrap-filter-params [{:keys [query]}]
  (fn [f]
    (f (assoc req :filters (coerce-filter-params (:params req) query)))))

(defn show-providers [_]
  {:body "show providers"
   :status 200})

(defn show [_]
  (ui/Page
    :title "Admin"
    :footer
    ;; TODO code splitting
    [:<> [:script {:src "/js/admin.js"}]]
    :content
    [:main
     [:section
      [:a {:href "/admin/appointments"}
       [:h2 "Appointments"]]
      [:a {:href "/admin/appointments?status=follow-up"}
       [:h2 (count $appointments) " Appointments need follow-up"]]
      [:a {:href "/admin/appointments?status=confirmed"}
       [:h3 (count $appointments) " confirmed"]] ;; TODO more accurate status?
      [:a {:href "/admin/appointments?status=archived"}
       [:h3 (count $appointments) " archived"]]
      [:h3 (count $appointments) " total"]]
     [:section
      [:h2 (count []) " providers"]]]))
