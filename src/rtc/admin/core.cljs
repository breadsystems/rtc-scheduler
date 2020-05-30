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
   [reagent.dom :as dom]
   [re-frame.core :as rf]
   [reitit.frontend :as reitit]
   [reitit.frontend.easy :as easy]
   [rtc.api :as api]
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
    :users []
    :current-invite {:email ""}
    :greeting "Hello, world!"
    :my-invitations []}))

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
  (reitit/router
   ["/comrades"
    [""
     {:name ::dashboard}]
    ["/new-careseekers"
     {:name ::new-careseekers}]
    ["/schedule"
     {:name ::schedule}]
    ["/invites"
     {:name ::invites}]
    ["/settings"
     {:name ::settings}]]))

(defn- init-routing! []
  (easy/start!
   routes
   (fn [match]
     (when match
       (rf/dispatch [::update-route (:data match)])))
   {:use-fragment false}))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;     Subscriptions     ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub ::current-view :current-view)

(comment
  @(rf/subscribe [::current-view]))



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
     (assoc db :my-invitations (:invitations data)))))

(comment
  (rf/dispatch [::init-admin]))


(defn dashboard []
  [:main
   [:p [:a {:href (easy/href ::dashboard)} "Dashboard"]]
   [:p [:a {:href (easy/href ::schedule)} "Schedule"]]
   [:p [:a {:href (easy/href ::invites)} "Invites"]]
   [:h2 "Dashboard"]
   (condp = (:name @(rf/subscribe [::current-view]))
     ::invites [invites/invites]
     ::schedule [:p "SCHEDULE HERE"]
     ::settings [:p "SETTINGS HERE"]
     [:p "DASH"])])




    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                           ;;
  ;;      MOUNT THE APP!!      ;;
 ;;                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn ^:dev/after-load mount! []
  (dom/render [dashboard] (.getElementById js/document "rtc-admin-app")))

(defn init! []
  (init-routing!)
  (rf/dispatch-sync [::init-db])
  (rf/dispatch [::init-admin])
  (mount!))