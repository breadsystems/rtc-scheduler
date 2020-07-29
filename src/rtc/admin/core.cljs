;;
;; This is the core Admin UI of the RTC scheduler app.
;;
;; In this namespace:
;; * Client-side DB (Re-frame)
;; * Client-side routes (Reitit)
;; * React/reagent initialization/mount
;;
(ns rtc.admin.core
  (:require
   [clojure.set :refer [intersection]]
   [reagent.dom :as dom]
   [re-frame.core :as rf]
   [reitit.frontend :as reitit]
   [reitit.frontend.easy :as easy]
   [rtc.admin.calendar :as calendar]
   [rtc.admin.schedule :as schedule]
   [rtc.api.core :as api]
   [rtc.users.invites.ui :as invites]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                           ;;
  ;;   Client DB and Routing   ;;
 ;;                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; This is where all the client-side application state lives.
;; We don't read or write this state data directly; instead,
;; we read it via re-frame subscriptions and write to it via
;; events: http://day8.github.io/re-frame/application-state/
;;
;; TODO: We PROMISED to make a leverageable schema...go do that
;; http://day8.github.io/re-frame/application-state/#create-a-leveragable-schema
;;
(rf/reg-event-db
 ::init-db
 (fn [_]
   {:view :schedule
    ;; TODO get this stuff from GraphQL
    :user-id 3
    :filters {:availabilities? true
              :appointments? true
              :providers #{3} ;; TODO
              :access-needs #{}}
    :availabilities {1 {:id 1
                        :start #inst "2020-07-27T09:00:00-07:00"
                        :end #inst "2020-07-27T16:00:00-07:00"
                        :event/type :availability
                        :user/id 3}
                     2 {:id 2
                        :start #inst "2020-07-29T09:00:00-07:00"
                        :end #inst "2020-07-29T16:00:00-07:00"
                        :event/type :availability
                        :user/id 3}
                     3 {:id 3
                        :start #inst "2020-07-30T10:00:00-07:00"
                        :end #inst "2020-07-30T15:00:00-07:00"
                        :event/type :availability
                        :user/id 3}
                     4 {:id 4
                        :start #inst "2020-08-01T11:00:00-07:00"
                        :end #inst "2020-08-01T15:00:00-07:00"
                        :event/type :availability
                        :user/id 3}
                     5 {:id 5
                        :start #inst "2020-08-02T10:00:00-07:00"
                        :end #inst "2020-08-02T16:00:00-07:00"
                        :event/type :availability
                        :user/id 2}
                     6 {:id 6
                        :start #inst "2020-08-01T11:00:00-07:00"
                        :end #inst "2020-08-01T15:00:00-07:00"
                        :title "AVAIL"
                        :event/type :availability
                        :user/id 2}}
    :appointments {10 {:id 10
                       :start #inst "2020-08-01T12:00:00-07:00"
                       :end #inst   "2020-08-01T12:30:00-07:00"
                       :name "Octavia"
                       :event/type :appointment
                       :user/id 3}
                   11 {:id 11
                       :start #inst "2020-07-30T13:00:00-07:00"
                       :end #inst   "2020-07-30T13:30:00-07:00"
                       :name "Malcom"
                       :event/type :appointment
                       :user/id 3}
                   12 {:id 12
                       :start #inst "2020-08-02T14:00:00-07:00"
                       :end #inst   "2020-08-02T14:30:00-07:00"
                       :name "Angela"
                       :event/type :appointment
                       :user/id 2}
                   13 {:id 13
                       :start #inst "2020-07-28T11:00:00-07:00"
                       :end #inst   "2020-07-28T11:30:00-07:00"
                       :name "Ursula"
                       :event/type :appointment
                       :user/id 1}}
    :users {3 {:id 3
               :first_name "Coby"
               :last_name "Tamayo"
               :roles #{:doc :kin}}}
    :current-invite {:email ""}
    :my-invitations []
    :focused-appointment nil}))

;;
;; Client-side routing, via Reitit.
;; This manages navigation, including browser history.
;; https://metosin.github.io/reitit/frontend/browser.html
;;
;; We don't use Controllers here, but if this starts to get messy,
;; we may want to.
;; https://metosin.github.io/reitit/frontend/controllers.html
;;
(def ^:private routes
  [[""
    {:name ::schedule
     :heading "📆 Care Schedule"}]
   ["/invites"
    {:name ::invites
     :heading "🎉 Invites"}]
   ["/settings"
    {:name ::settings
     :heading "⚙ Account Settings"}]])

(defn- init-routing! []
  (easy/start!
   (reitit/router
    ;; Build up our routes like this: ["/comrades" ["" {:name ::dashboard ...}] ...]
    (concat ["/comrades"] routes))
   (fn [match]
     (when match
       (rf/dispatch [::update-route (:data match)])))
   ;; Use the HTML5 History API, not URL fragments
   {:use-fragment false}))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;        Helpers        ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn accessible-by?
  "Whether the given roles grant a user access to resource"
  [user-roles resource]
  (let [required-roles (:restrict-to-roles resource)]
    (or (empty? required-roles)
        (> (count (intersection required-roles user-roles)) 0))))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;     Subscriptions     ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub ::current-view :current-view)
(rf/reg-sub ::routes (fn [db [_ routes]]
                       (as-> routes $
                         (map second $)
                         (filter (partial accessible-by? (:my-roles db)) $))))

(rf/reg-sub :admin/users :users)

(comment
  routes
  @(rf/subscribe [::current-view])
  @(rf/subscribe [::routes routes])
  @(rf/subscribe [:admin/users]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;        Events         ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; Dispatched when the user visits a client-side route (including on
;; initial page load, once the router figures out what page we're on).
(rf/reg-event-db
 ::update-route
 (fn [db [_ view]]
   (assoc db :current-view view)))

;; Dispatched on initial page load
(rf/reg-event-fx
 ::init-admin
 (fn []
   (api/query! [:query [:invitations
                        {:redeemed false}
                        :email
                        :date_invited]]
               ::admin-data)))

;; Dispatched once the admin data is ready
(rf/reg-event-db
 ::admin-data
 (fn [db [_ {:keys [data errors]}]]
   (if (seq errors)
     ;; TODO some kind of real error handling
     (js/console.error errors)
     (assoc db
            :my-invitations (:invitations data)
            ;; :availabilities (:availabilities data)
            ;; :appointments   (:appointments data)
            ))))

(comment
  (rf/dispatch [::init-admin]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;      Components       ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- main-nav []
  (let [my-routes @(rf/subscribe [::routes routes])]
    [:nav
     [:ul
      (map (fn [{:keys [name heading nav-title]}]
             ^{:key name}
             [:li {}
              [:a.nav-link {:href (easy/href name)} (or nav-title heading)]])
           my-routes)
      [:li [:a.nav-link.logout {:href "/logout"} "🔑 Logout"]]]]))

(defn admin-ui []
  (let [{:keys [name heading]} @(rf/subscribe [::current-view])]
    [:div.container.container--comrades
     [:header
      [:h1 heading]
      [main-nav]]
     [:main
      (condp = name
        ::welcome [:p "Welcome!"]
        ::new-careseekers [:p "TODO NEW CARESEEKERS"]
        ::schedule [calendar/care-schedule]
        ::invites [invites/invites]
        ::settings [:p "TODO SETTINGS HERE"]
        [:p "Uh oh, that page was not found!"])]]))




    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                           ;;
  ;;      MOUNT THE APP!!      ;;
 ;;                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn ^:dev/after-load mount! []
  (dom/render [admin-ui] (.getElementById js/document "rtc-admin-app")))

(defn init! []
  (init-routing!)
  (rf/dispatch-sync [::init-db])
  (rf/dispatch [::init-admin])
  (mount!))