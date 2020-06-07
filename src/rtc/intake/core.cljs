(ns rtc.intake.core
  (:require
   [clojure.string :refer [join]]
   ["moment" :as moment]
   ["@fullcalendar/react" :default FullCalendar]
   ["@fullcalendar/daygrid" :default dayGridPlugin]
   ["@fullcalendar/timegrid" :default timeGridPlugin]
   [reagent.dom :as dom]
   [re-frame.core :as rf]
   [reitit.frontend :as reitit]
   [reitit.frontend.easy :as easy]
   [rtc.api.core :as api]
   [rtc.i18n.core :as i18n]
   [rtc.util :refer [->opt]]))



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
   {:step 0
    :lang :en
    ;; Where we collect info about the Person Seeking Care.
    :careseeker-info {}
    :appointment-windows []
    :answers {}
    ;; TODO farm this out to an EDN file to make it more configurable?
    :steps
    [{:name :basic-info
      :questions
      [{:key :name
        :help :name-help
        :type :text}
       {:key :pronouns
        :placeholder :they-them
        :type :text}
       {:key :state
        :help :state-help
        :type :select
        :required? true
        :options :states}]}
     {:name :contact-info
      :questions
      [{:key :email
        :type :email}
       {:key :phone
        :type :text}
       {:key :text-ok
        :help :text-ok-help
        :type :radio
        :options :yes-no}
       {:key :preferred-communication-method
        :type :radio
        :options :communication-methods}]}
     {:name :access-needs
      :questions
      [{:key :interpreter-lang
        :help :interpreter-lang-help
        :type :select
        :options :lang-options}
       {:key :other-access-needs
        :type :text
        :options :lang-options}]}
     {:name :medical-needs
      :questions
      [{:key :description-of-needs
        :help :description-of-needs-help
        :type :text}
       {:key :anything-else
        :type :text}]}
     {:name :schedule}]
    :i18n
    ;; TODO how to deal with groupings like this?
    {:en {:yes-no [{:value 1 :label "Yes"}
                   {:value 0 :label "No"}]
          :communication-methods [{:value "phone" :label "Phone"}
                                  {:value "email" :label "Email"}]
          :name "Name"
          :name-help "Not required."
          :pronouns "Pronouns"
          :they-them "they/them/theirs"
          :state "States"
          :state-help "Needed to find a provider who can legally provide care for you."
          :basic-info "Basic Info"
          :contact-info "Contact Info"
          :access-needs "Access Needs"
          :medical-needs "Medical Needs"
          :email "Email"
          :phone "Phone"
          :text-ok "OK to text?"
          :text-ok-help "Depending on your carrier, you may incur charges."
          :preferred-communication-method "Preferred Communcation Method"
          :schedule "Schedule an Appointment"
          :interpreter-lang "Do you need an interpreter?"
          :interpreter-lang-help "If you don't need an interpreter, leave this blank."
          :lang-options [{:value nil :label "Choose..."}
                         "Amharic"
                         "Arabic"
                         "ASL - American Sign Language"
                         "Chinese Cantonese"
                         "Chinese Madorin"
                         "Khmer"
                         "Korean"
                         "Punjabi"
                         "Russian"
                         "Spanish"
                         "Somali"
                         "Tagalog"
                         "Ukrainian"
                         "Vietnamese"
                         {:value :other :label "Other..."}]
          :other-access-needs "Any other access needs we can assist you with?"
          :description-of-needs "Short Description of Medical Needs"
          :description-of-needs-help "For example, \"fever and sore throat for 3 days,\" or \"insulin prescription\""
          :anything-else "Anything we forgot to ask?"
          :states
          [{:value ""   :label "Choose a state"}
           {:value "WA" :label "Washington"}
           {:value "NY" :label "New York"}
           {:value "CA" :label "California"}]}
     :es {:yes-no [{:value 1 :label "Si"}
                   {:value 0 :label "Yes"}]
          :communication-methods [{:value "phone" :label "Telefono"}
                                  {:value "email" :label "Email"}]
          :name "Nombre"
          :pronouns "Pronombres"
          :state "Estado"}}}))

;;
;; Client-side routing, via Reitit.
;; This manages navigation, including browser history.
;; https://metosin.github.io/reitit/frontend/browser.html
;;
;; We don't use Controllers here, but if this starts to get messy,
;; we may want to.
;; https://metosin.github.io/reitit/frontend/controllers.html
;;
(def ^:private parent-route "/get-care")
(def ^:private routes
  [[""
    {:name :basic-info}]
   ["/contact"
    {:name :contact-info}]
   ["/access-needs"
    {:name :access-needs}]
   ["/medical-needs"
    {:name :medical-needs}]
   ["/schedule"
    {:name :schedule}]])

(defn- nested-route [child]
  (str parent-route child))

(defn- init-routing! []
  (easy/start!
   (let [indexed-routes (map-indexed (fn [idx route]
                                       (update route 1 #(assoc % :step idx)))
                                     routes)]
     (reitit/router
     ;; Build up our routes like this: ["/get-care" ["" {:name :basic-info ...}] ...]
      (concat [parent-route] indexed-routes)))
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

(defn current-step [{:keys [step steps]}]
  (get steps step))

(defn current-questions [db]
  (get (current-step db) :questions))

(defn accessible-routes [{:keys [step viewed-up-to-step]} routes]
  (map (fn [route]
         (let [view (second route)]
           (assoc view
                  :current? (= step (:step view))
                  :viewed? (>= viewed-up-to-step (:step view)))))
       routes))

(defn answer [{:keys [answers]} [_ k]]
  (get answers k ""))

;; Get routes in a more easily consumable format
(rf/reg-sub ::routes (fn [db]
                       (accessible-routes db routes)))
(rf/reg-sub ::appointment-windows :appointment-windows)
(rf/reg-sub ::current-step current-step)
(rf/reg-sub ::questions current-questions)
(rf/reg-sub ::answer answer)
(rf/reg-sub ::i18n (fn [db [_ phrase-key]]
                     (i18n/t db phrase-key)))

(comment
  routes
  @(rf/subscribe [::questions])
  @(rf/subscribe [::current-step])

  @(rf/subscribe [::answer :name])
  (rf/dispatch [::answer! :name "Coby"])

  @(rf/subscribe [::answer :state])

  ;; view heading for "Access Needs"
  @(rf/subscribe [::i18n :access-needs])
  ;; => "Access Needs"

  ;; current view heading
  (let [{phrase-key :name} @(rf/subscribe [::current-step])]
    @(rf/subscribe [::i18n phrase-key]))

  @(rf/subscribe [::routes]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;    Re-frame Events    ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn update-route [db [_ {:keys [step]}]]
  (assoc db :step step))

(defn update-location!
  "Related to update-route, but only responsible for (effectfully) updating window Location.
   Does not affect the db."
  [step]
  (let [route (nested-route (first (get routes step)))]
    (.pushState js/window.history #js {} nil route)))

(defn update-answer [db [_ k v]]
  (assoc-in db [:answers k] v))

(defn next-step [{:keys [db]}]
  (let [{:keys [step steps viewed-up-to-step]} db
        new-step (min (inc step) (dec (count steps)))]
    {:db (assoc db
                :step new-step
                :viewed-up-to-step (max viewed-up-to-step new-step))
     ::location new-step}))

(defn prev-step [{:keys [db]}]
  (let [{:keys [step]} db
        new-step (max (dec step) 0)]
    {:db (assoc db :step new-step)
     ::location new-step}))


;; Dispatched when the user visits a client-side route (including on
;; initial page load, once the router figures out what page we're on).
(rf/reg-event-db ::update-route update-route)

(rf/reg-fx ::location update-location!)

(rf/reg-event-db ::answer! update-answer)
(rf/reg-event-fx ::prev-step prev-step)
(rf/reg-event-fx ::next-step next-step)

;; Dispatched on initial page load
(defn- *generate-calendar-events [cnt]
  (doall (distinct (map (fn [_]
                          (let [m (doto (moment)
                                    (.add (rand-int 3) "hours")
                                    (.subtract (rand-int 6) "hours")
                                    (.add (inc (rand-int 20)) "days"))
                                start (.format m "YYYY-MM-DDTHH:00")
                                end   (.format m "YYYY-MM-DDTHH:30")]
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

  (rf/dispatch [::update-route {:name :basic-info :step 0}])
  (rf/dispatch [::update-route {:name :contact-info :step 1}])
  (rf/dispatch [::update-route {:name :schedule :step 4}]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;        Helpers        ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn t [k]
  @(rf/subscribe [::i18n k]))



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
    [:button {:on-click #(rf/dispatch [::prev-step])} "Back"]
    [:button {:on-click #(rf/dispatch [::next-step])} "Next"]]])

(defn- question [{:keys [key type help required? placeholder options] :as q}]
  [:div.question
   [:label (t key)]
   [:div.field
    (case type
      :text
      [:input {:type :text
               :value @(rf/subscribe [::answer key])
               :placeholder (t placeholder)
               :required required?
               :on-change #(rf/dispatch [::answer! key (.. % -target -value)])}]

      :email
      [:input {:type :email
               :value @(rf/subscribe [::answer key])
               :placeholder (t placeholder)
               :required required?
               :on-change #(rf/dispatch [::answer! key (.. % -target -value)])}]

      :radio
      [:<>
       (map (fn [{:keys [value label]}]
              (let [id (str (name key) "-" value)]
                ^{:key value}
                [:<>
                 [:input {:id id :name (name key) :type :radio}]
                 [:label {:for id} label]]))
            (t options))]

      :select
      [:select {:value @(rf/subscribe [::answer key])
                :on-change #(rf/dispatch [::answer! key (.. % -target -value)])}
       (map (fn [opt]
              (let [{:keys [value label]} (->opt opt)]
                ^{:key value}
                [:option {:value value} label]))
            (t options))]

      [:span "TODO"])]
   (when help [:p.help (t help)])])

(defn- questions []
  (let [step (:name @(rf/subscribe [::current-step]))
        qs @(rf/subscribe [::questions step])]
    [intake-step
     {:heading (t step)
      :content (map (fn [q]
                      ^{:key (:key q)}
                      [question q])
                    qs)}]))

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
      (map (fn [{:keys [name current? viewed?]}]
             (let [nav-title (t name)]
               ^{:key name}
               [:li {:class (join " " [(when current? "current") (when viewed? "viewed")])}
                [:a {:href (easy/href name)} nav-title]]))
           nav-routes)]]))

(defn intake-ui []
  (let [{:keys [name]} @(rf/subscribe [::current-step])]
    [:div.container.container--get-care
     [:header
      [:h1 "Radical Telehealth Collective"]
      [:h2 "Get Care"]
      [main-nav]]
     [:main
      (if (= :schedule name)
        [schedule]
        [questions])]]))


(defn ^:dev/after-load mount! []
  ;; TODO load from GraphQL
  (rf/dispatch [::load-appointment-windows (*generate-calendar-events 100)])
  (dom/render [intake-ui] (.getElementById js/document "rtc-intake-app")))

(defn init! []
  (init-routing!)
  (rf/dispatch-sync [::init-db])
  (mount!))