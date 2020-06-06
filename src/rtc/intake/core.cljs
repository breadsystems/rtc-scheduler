(ns rtc.intake.core
  (:require
   ["moment" :as moment]
   ["@fullcalendar/react" :default FullCalendar]
   ["@fullcalendar/daygrid" :default dayGridPlugin]
   ["@fullcalendar/timegrid" :default timeGridPlugin]
   [reagent.dom :as dom]
   [re-frame.core :as rf]
   [reitit.frontend :as reitit]
   [reitit.frontend.easy :as easy]
   [rtc.api.core :as api]))



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
   ;; Just make first view the default?
   {:current-view {:name ::basic-info
                   :nav-title "Basic Info"}
    ;; Where we collect info about the Person Seeking Care.
    :careseeker-info {}
    :appointment-windows []
    ;; TODO farm this out to an EDN file to make it more configurable?
    :steps
    {::basic-info {:name ::basic-info
                   :questions
                   [{:key :name
                     :type :text}
                    {:key :pronouns
                     :type :text}
                    {:key :state
                     :type :select
                     :options ::states}]}
     ::contact-info {:name ::contact-info
                     :questions
                     [{:key :email
                       :type :email}
                      {:key :phone
                       :type :text}
                      {:key :text-ok?
                       :type :radio
                       :options :yes-no}
                      {:key :preferred-communication-method
                       :type :radio
                       :options :communication-methods}]}
     ::access-needs {:name ::access-needs
                     :questions
                     [{:key :interpreter-lang
                       :type :select
                       :options :lang-options}]}
     ::medical-needs {:name ::medical-needs
                      :questions
                      [{:key :description-of-needs
                        :type :text}
                       {:key :anything-else
                        :type :text}]}}
    :i18n
    ;; TODO how to deal with groupings like this?
    {:en {:yes-no [{:value 1 :label "Yes"}
                   {:value 0 :label "No"}]
          :communication-methods [{:value "phone" :label "Phone"}
                                  {:value "email" :label "Email"}]
          :name "Name"
          :pronouns "Pronouns"
          :state "States"}
     :es {:yes-no [{:value 1 :label "Si"}
                   {:value 0 :label "Yes"}]
          :communication-methods [{:value "phone" :label "Telefono"}
                                  {:value "email" :label "Email"}]
          :name "Nombre"
          :pronouns "Pronombres"
          :state "Estado"}}
    :states
    [{:value "WA" :label "Washington"}
     {:value "NY" :label "New York"}
     {:value "CA" :label "California"}]}))

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
    {:name ::basic-info
     :nav-title "Basic Info"}]
   ["/contact"
    {:name ::contact-info
     :nav-title "Contact Info"}]
   ["/access-needs"
    {:name ::access-needs
     :nav-title "Access Needs"}]
   ["/medical-needs"
    {:name ::medical-needs
     :nav-title "Medical Needs"}]
   ["/schedule"
    {:name ::schedule
     :nav-title "Schedule an Appointment"}]])

(defn- init-routing! []
  (easy/start!
   (reitit/router
    ;; Build up our routes like this: ["/get-care" ["" {:name ::basic-info ...}] ...]
    (concat ["/get-care"] routes))
   (fn [match]
     (when match
       (rf/dispatch [::update-route (:data match)])))
   ;; Use the HTML5 History API, not URL fragments
   {:use-fragment false}))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;     Subscriptions     ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub ::current-view :current-view)

;; Get routes in a more easily consumable format
(rf/reg-sub ::routes (fn [{:keys [current-view]}]
                       (map (fn [route]
                              ;; First extract the view data from the route
                              (let [view (second route)]
                                (assoc view :current? (= (:name current-view) (:name view)))))
                            routes)))

(rf/reg-sub ::appointment-windows :appointment-windows)

(rf/reg-sub ::questions (fn [{:keys [steps]} [_ view]]
                          (get-in steps [view :questions])))

(rf/reg-sub ::t (fn [{:keys [lang i18n]} [_ phrase]]
                  (get-in i18n [lang phrase])))

(comment
  routes
  @(rf/subscribe [::questions ::basic-info])
  @(rf/subscribe [::current-view])
  @(rf/subscribe [::routes]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;    Re-frame Events    ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; Dispatched when the user visits a client-side route (including on
;; initial page load, once the router figures out what page we're on).
(rf/reg-event-db
 ::update-route
 (fn [db [_ view]]
   (assoc db :current-view view)))

;; Dispatched on initial page load
(defn- *generate-calendar-events [cnt]
  (doall (distinct (map (fn [_]
                          (let [m (doto (moment)
                                    (.add (rand-int 3) "hours")
                                    (.subtract (rand-int 6) "hours")
                                    (.add (inc (rand-int 20)) "days"))
                                start (.format m "YYYY-MM-DDTHH:00")
                                end   (.format m "YYYY-MM-DDTHH:30")]
                            (js/console.log start end)
                            {:start start
                             :end end
                             :allDay false}))
                        (range 0 cnt)))))

(rf/reg-event-db
 ::load-appointment-windows
 (fn [db [_ windows]]
   (assoc db :appointment-windows windows)))

(comment
  ;; Moment.js experiments
  (moment)
  (.format (moment) "YYYY-MM-DD")
  (doto (moment)
    (.add (rand-int 10) "days")
    (.add (rand-int 4) "hours"))

  (rf/dispatch [::update-route {:name ::basic-info
                                :nav-title "Basic Info"}])
  (rf/dispatch [::update-route {:name ::contact-info
                                :nav-title "Contact Info"}])
  (rf/dispatch [::update-route {:name ::schedule
                                :nav-title "Schedule an Appointment"}]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;      Components       ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- intake-step [{:keys [heading sub-heading content]}]
  [:section
   [:header
    [:h3 heading]
    (when sub-heading [:h4 sub-heading])]
   [:div
    content]
   [:footer
    [:p "Back / Next buttons go here..."]]])

(defn- questions [step]
  (let [qs @(rf/subscribe [::questions step])]
    [intake-step
     {:heading "TODO heading here..."
      :sub-heading "TODO sub-heading here..."
      :content (map (fn [q]
                      [:p (clj->js (:key q))])
                    qs)}]))

(defn- basic-info []
  [:p "Basic questions..."])

(defn- contact-info []
  [:p "Contact questions..."])

(defn- access-needs []
  [:p "Access Needs..."])

(defn- medical-needs []
  [:p "Medical Needs..."])

(defn- schedule []
  (let [windows @(rf/subscribe [::appointment-windows])]
    (intake-step
     {:heading
      "Select a time by clicking on one of the available appointment windows"
      :sub-heading
      "These are the appointment times available for residents of your state."
      :content
      [:> FullCalendar {:default-view "timeGridWeek"
                        :events windows
                        :plugins [dayGridPlugin timeGridPlugin]}]})))


(defn- main-nav []
  (let [nav-routes @(rf/subscribe [::routes])]
    [:nav
     [:ul
      (map (fn [{:keys [name nav-title current?]}]
             ^{:key name}
             [:li {:class (when current? "current")}
              [:a {:href (easy/href name)} nav-title]])
           nav-routes)]]))

(defn intake-ui []
  (let [{:keys [name nav-title]} @(rf/subscribe [::current-view])]
    [:div.container.container--get-care
     [:header
      [:h2 nav-title]
      [main-nav]]
     [:main
      ;; TODO make this more extensible
      (condp = name
        ::basic-info [questions ::basic-info]
        ::contact-info [contact-info]
        ::access-needs [access-needs]
        ::medical-needs [medical-needs]
        ::schedule [schedule])]]))


(defn ^:dev/after-load mount! []
  ;; TODO load from GraphQL
  (rf/dispatch [::load-appointment-windows (*generate-calendar-events 100)])
  (dom/render [intake-ui] (.getElementById js/document "rtc-intake-app")))

(defn init! []
  (init-routing!)
  (rf/dispatch-sync [::init-db])
  (mount!))