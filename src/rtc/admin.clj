(ns rtc.admin
  (:require
    [rtc.ui :as ui])
  (:import
    [java.text SimpleDateFormat]))

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

(defn AdminPage [& {:keys [footer system] :as req}]
  (ui/Page (assoc req
                  :footer
                  [:<>
                   footer
                   (DebugFooter system)])))

(defn show [_]
  (ui/Page
    :title "Admin"
    :footer
    ;; TODO code splitting
    [:<> [:script {:src "/js/admin.js"}]]
    :content
    [:main.spacious
     [:article.card
      [:h1 "Appointments"]
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
