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
   [rtc.rest.core :as rest]
   [rtc.style.colors :as colors]
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
 (fn [_db [_ {:keys [csrf-token]}]]
   {;; CSRF token from the DOM
    :csrf-token csrf-token
    
    :view :schedule
    ;; TODO get this stuff from GraphQL
    :user-id 4
    :filters {:availabilities? true
              :appointments? true
              :providers #{}
              :access-needs #{}}
    :availabilities {}
    :appointments {}
    :users {}
    :needs {1 {:id 1 :need/type :interpreter :name "Interpreter"}
            2 {:id 2 :need/type :closed-captioning :name "Closed Captioning"}}
    :current-invite {:email ""}
    :my-invitations []
    :focused-appointment nil
    :colors colors/availability-colors}))

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
(rf/reg-event-fx
 ::update-route
 (fn [{:keys [db]} [_ view]]
   (let [effect (:name view)]
     {:db (assoc db :current-view view)
      effect []})))

;; Dispatched on initial page load
(rf/reg-event-fx
 ::init-admin
 (fn [_]
   (let [event (:name @(rf/subscribe [::current-view]))]
     {event []})))

(rf/reg-fx
 ::schedule
 (fn []
   (rest/get! "/api/v1/admin/schedule" {} :calendar/load)))

;; TODO implement invites & settings fx
(rf/reg-fx
 ::invites
 #(prn 'invites!))

(rf/reg-fx
 ::settings
 #(prn 'settings!))

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
  (let [csrf-token (.-content (js/document.querySelector "meta[name=csrf-token]"))]
    (rf/dispatch-sync [::init-db {:csrf-token csrf-token}]))
  (rf/dispatch [::init-admin])
  (mount!))