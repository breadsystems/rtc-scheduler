(ns rtc.admin
  (:require
    [rtc.ui :as ui])
  (:import
    [java.text SimpleDateFormat]))

(defn coerce-filter-params [params coercions]
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

(defn DebugFooter [{:app/keys [clojure-version
                               git-hash
                               release-version
                               started-at]}]
  [:.bottom
   (str "Release version: " git-hash
        " | Started at: " (when started-at
                            (.format fmt-ymd started-at))
        " | Clojure version: " clojure-version)])

(defn AdminPage [& {:keys [footer system session] :as req}]
  (when-not session
    (throw (ex-info "No :session key!" {:keys (keys req)})))
  (when-not system
    (throw (ex-info "No :system key!" {:keys (keys req)})))
  (ui/Page
    (assoc req
           :head
           [:link {:rel :stylesheet :href "/admin/admin.css"}]
           :banner
           (when (false? (get-in system [:app/authentication :enabled?]))
             [:.banner-alert
              [:strong "WARNING: Authentication is disabled!"]])
           :nav
           [:nav
            [:a {:href "/admin"} [:h1 "RTC"]]
            [:ul
             [:li [:a {:href "/admin/appointments"} "Appointments"]]
             #_
             [:li [:a {:href "/admin/schedulers"} "Schedulers"]]
             #_
             [:li [:a {:href "/admin/providers"} "Providers"]]
             [:li [:a {:href "/account"} "My account"]]]]
           :footer
           [:<>
            footer
            (DebugFooter system)])))

(defn show [req]
  (AdminPage
    (assoc req
           :title "Admin"
           :footer
           ;; TODO remove?
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
             [:div.instruct "Provider roster coming soon"]]])))
