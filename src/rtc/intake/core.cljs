(ns rtc.intake.core
  (:require
   [clojure.string :refer [join]]
   ["moment" :as moment]
   ["moment/locale/es"]
   ["@fullcalendar/react" :default FullCalendar]
   ["@fullcalendar/timegrid" :default timeGridPlugin]
   ["@fullcalendar/list" :default listPlugin]
   [reagent.dom :as dom]
   [re-frame.core :as rf]
   [rtc.api.core :as api]
   [rtc.i18n.core :as i18n]
   [rtc.i18n.data :as i18n-data]
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
    :viewed-up-to-step 0
    :lang :en
    ;; TODO load these from i18n
    :lang-options [{:value :en :label "English"}
                   {:value :es :label "Español"}]
    :loading? false
    ;; Where we collect info about the Person Seeking Care.
    :careseeker-info {}
    :appointment-windows []
    :answers {}
    :touched #{}
    :confirmed-info nil
    ;; TODO farm this out to an EDN file to make it more configurable?
    :steps
    [{:name :basic-info
      :questions
      [{:key :name
        :help :name-help
        :placeholder :anonymous
        :confirm-fallback :anonymous
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
        :help :email-or-phone-help
        :required-without-any? [:phone]
        :required-message [:email-or-phone]
        :type :email}
       {:key :phone
        :help :email-or-phone-help
        :required-without-any? [:email]
        :required-message [:email-or-phone]
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
        :options :interpreter-options}
       {:key :other-access-needs
        :type :text
        :options :interpreter-options}]}
     {:name :medical-needs
      :questions
      [{:key :description-of-needs
        :help :description-of-needs-help
        :required? true
        :required-message [:please-describe-medical-needs]
        :type :text}
       {:key :anything-else
        :type :text}]}
     {:name :schedule}
     {:name :confirmation}]
    :i18n (i18n-data/i18n-data)}))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;     Subscriptions     ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-step [{:keys [step steps]}]
  (get steps step))

(defn current-questions [db]
  (get (current-step db) :questions))

(defn filter-errors-for [errors k]
  (filter #(= k (:for %)) errors))

(defn question->validator [q]
  (cond
    (:required? q)
    (fn [answers _]
      (if (empty? (get answers (:key q)))
        [{:for (:key q)
          :message (or (:required-message q) [:please-enter (:key q)])}]
        []))

    (:required-without-any? q)
    (fn [answers _]
      (let [other-answers (map (fn [k] (get answers k)) (:required-without-any? q))
            this-answer (get answers (:key q))]
        ;; If any of these *other* answers are blank, we need *this* answer.
        (if (and (empty? this-answer) (not (every? (complement empty?) other-answers)))
          [{:for (:key q)
            :message (or (:required-message q) [:please-enter (:key q)])}]
          [])))

    :else
    (constantly [])))

(defn questions->errors [qs answers]
  (let [validators (map question->validator qs)]
    (reduce concat (map (fn [valid?] (valid? answers qs)) validators))))

(defn current-errors [{:keys [answers] :as db}]
  (questions->errors (current-questions db) answers))

(defn errors-for [{:keys [touched] :as db} [_ k]]
  (if (contains? touched k)
    (filter-errors-for (current-errors db) k)
    []))

(defn display-errors-for? [{:keys [touched]} k]
  (contains? touched k))

(defn step-valid? [{:keys [answers] :as db}]
  ;; Each validator returns a list of errors. Check that each list is empty.
  (empty? (questions->errors (current-questions db) answers)))

(defn can-go-prev? [{:keys [step]}]
  (> step 0))

(defn can-go-next? [{:keys [step steps] :as db}]
  (and
   (> (count steps) (inc step))
   (step-valid? db)))

(defn last-step? [{:keys [step steps]}]
  (>= step (dec (count steps))))

(defn accessible-steps [{:keys [step steps viewed-up-to-step] :as db}]
  (map-indexed (fn [idx view]
                 (let [current? (= step idx)
                       viewed? (>= viewed-up-to-step idx)
                       valid? (step-valid? db)
                       is-next? (= (inc step) idx)]
                   (assoc view
                          :step idx
                          :current? current?
                          :accessible? (or (> step idx) current? (and (or is-next? viewed?) valid?))
                          :viewed? viewed?)))
               steps))

(defn answer [{:keys [answers]} [_ k]]
  (get answers k ""))

(defn confirmation-values [{:keys [answers steps] :as db}]
  (let [question->value (fn [{:keys [key confirm-fallback options]}]
                          (let [answer (get answers key)]
                            (if options
                              ;; Translate option value back into its human-readable label
                              (let [value->label (reduce #(assoc %1 (:value %2) (:label %2))
                                                         {}
                                                         (i18n/t db options))]
                                (get value->label answer))
                              ;; User answered in their own language...
                              (or answer
                                  ;; ...OR we translate the fallback key
                                  (i18n/t db confirm-fallback)))))
        questions->values (fn [values q]
                            (conj values (let [v (question->value q)]
                                           (when v {(:key q) v}))))
        questions (reduce (fn [qs {:keys [questions]}]
                            (concat qs questions))
                          {}
                          steps)]
    (reduce questions->values {} questions)))


(rf/reg-sub ::loading? :loading?)
(rf/reg-sub ::steps accessible-steps)
(rf/reg-sub ::appointment-windows :appointment-windows)
(rf/reg-sub ::current-step current-step)
(rf/reg-sub ::questions current-questions)
(rf/reg-sub ::can-go-prev? can-go-prev?)
(rf/reg-sub ::can-go-next? can-go-next?)
(rf/reg-sub ::last-step? last-step?)
(rf/reg-sub ::errors current-errors)
(rf/reg-sub ::errors-for errors-for)
(rf/reg-sub ::touched :touched)
(rf/reg-sub ::answers :answers)
(rf/reg-sub ::answer answer)
(rf/reg-sub ::confirmation-values confirmation-values)
(rf/reg-sub ::i18n (fn [db [_ phrase-key]]
                     (i18n/t db phrase-key)))
(rf/reg-sub ::lang :lang)
(rf/reg-sub ::lang-options :lang-options)

(rf/reg-sub ::appointment :appointment)
(rf/reg-sub ::confirmed-info :confirmed-info)

(comment
  (rf/dispatch [::init-db])

  @(rf/subscribe [::steps])
  @(rf/subscribe [::questions])
  @(rf/subscribe [::current-step])
  @(rf/subscribe [::last-step?])
  @(rf/subscribe [::errors])

  @(rf/subscribe [::can-go-next?])

  @(rf/subscribe [::answers])
  @(rf/subscribe [::answer :name])
  (rf/dispatch [::answer! :name "Coby"])

  @(rf/subscribe [::answer :state])
  @(rf/subscribe [::errors-for :state])
  (map :message @(rf/subscribe [::errors-for :state]))

  @(rf/subscribe [::touched])
  (rf/dispatch [::touch! :name])
  (rf/dispatch [::touch! :state])

  ;; view heading for "Access Needs"
  @(rf/subscribe [::i18n :access-needs])
  ;; => "Access Needs"

  @(rf/subscribe [::i18n :states])
  @(rf/subscribe [::i18n :description-of-needs])

  @(rf/subscribe [::lang])
  @(rf/subscribe [::confirmed-info])
  @(rf/subscribe [::appointment])

  ;; current view heading
  (let [{phrase-key :name} @(rf/subscribe [::current-step])]
    @(rf/subscribe [::i18n phrase-key]))

  @(rf/subscribe [::steps])

  ;; Fill out required stuff and jump to last step
  (do
    (rf/dispatch [::answer! :state "WA"])
    (rf/dispatch [::answer! :email "coby@tamayo.email"])
    (rf/dispatch [::answer! :description-of-needs "Life is pain"])
    (rf/dispatch [::update-step {:name :confirmation :step 5}]))

  (rf/dispatch [::confirm!]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;    Re-frame Events    ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn update-step [db [_ {:keys [step]}]]
  (assoc db :step step))

(defn update-answer [db [_ k v]]
  (assoc-in db [:answers k] v))

(defn next-step [{:keys [step steps viewed-up-to-step] :as db}]
  (let [new-step (min (inc step) (dec (count steps)))]
    (assoc db
           :step new-step
           :viewed-up-to-step (max viewed-up-to-step new-step))))

(defn prev-step [{:keys [step] :as db}]
  (let [new-step (max (dec step) 0)]
    (assoc db :step new-step)))

(defn touch [db [_ k]]
  (update db :touched conj k))


(defn fc-event->appointment [fc-event]
  {:start (.-start fc-event)
   :end   (.-end fc-event)
   :provider_id (.. fc-event -extendedProps -provider_id)})

(defn update-appointment [db [_ appt]]
  (assoc db :appointment appt))


(rf/reg-event-db ::update-step update-step)

(rf/reg-event-db ::answer! update-answer)
(rf/reg-event-db ::prev-step prev-step)
(rf/reg-event-db ::next-step next-step)
(rf/reg-event-db ::touch! touch)
(rf/reg-event-db ::update-appointment update-appointment)

(rf/reg-event-fx ::update-lang (fn [{:keys [db]} [_ lang]]
                                 {:db (assoc db :lang lang)
                                  :moment-locale lang}))

(rf/reg-fx :moment-locale (fn [lang]
                            (.locale moment (name lang))))

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
                             :allDay false
                             :provider_id 123}))
                        (range 0 cnt)))))

(rf/reg-event-db
 ::load-appointment-windows
 (fn [db [_ windows]]
   (assoc db :appointment-windows windows)))

(rf/reg-fx
 ::query
 (fn [[query event]]
   (api/query! query event)))

(rf/reg-event-fx
 ::confirm!
 (fn [{:keys [db]}]
   (let [{:keys [answers loading? confirmed-info]} db
         should-mutate? (and (not loading?) (not confirmed-info))]
     ;; Dispatching this event when the UI is already loading or an appointment
     ;; has already been confirmed is a noop.
     (when should-mutate?
       {:db (assoc db :loading? true)
        ;; TODO refine this mutation query
        ::query [[:mutation [:schedule (merge answers {:provider_id 123
                                                       :start_time "TODO"
                                                       :end_time "TODO"})
                             :email]]
                 ::confirmed]}))))

(rf/reg-event-db
 ::confirmed
 (fn [db [_ response]]
   (js/console.log (clj->js response))
   (assoc db
          :loading? false
          :confirmed-info response)))

(comment
  ;; Moment.js experiments
  (moment)
  (.format (moment) "YYYY-MM-DD")
  (.format (doto (moment)
             (.add (rand-int 10) "days")
             (.add (rand-int 4) "hours"))
           "dddd, MMMM Do")

  (rf/dispatch [::update-step {:name :basic-info :step 0}])
  (rf/dispatch [::update-step {:name :contact-info :step 1}])
  (rf/dispatch [::update-step {:name :schedule :step 4}])
  (rf/dispatch [::update-step {:name :confirmation :step 5}])
  
  (rf/dispatch [::update-lang :en])
  (rf/dispatch [::update-lang :es]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;        Helpers        ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn t [k]
  @(rf/subscribe [::i18n k]))

(defn t* [ks]
  (join " " (map t ks)))

(defn appointment->str [{:keys [start end]}]
  (let [s (moment start "en_US")
        e (moment end "en_US")]
    (str (.format s "h:mma - ") (.format e "h:mma ") (.format s "dddd, MMM Do"))))

(comment
  (t :name)
  (t* [:please-enter :name])
  (map (comp t* :message) @(rf/subscribe [::errors-for :state]))
  
  (appointment->str {:start "2020-07-06 16:00:00"
                     :end "2020-07-06 16:30:00"}))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;      Components       ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- intake-step [{:keys [sub-heading content]}]
  (let [show-next? (not @(rf/subscribe [::last-step?]))]
    [:section
     [:header
      (when sub-heading [:h3 (t sub-heading)])]
     [:div
      content]
     [:footer.intake-footer
      [:button.prev {:on-click #(rf/dispatch [::prev-step])
                     :disabled (not @(rf/subscribe [::can-go-prev?]))} (t :back)]
      (when show-next?
        [:button.next {:on-click #(rf/dispatch [::next-step])
                       :disabled (not @(rf/subscribe [::can-go-next?]))} (t :next)])]]))

(defn- question [{:keys [key type help required? required-without-any? placeholder options] :as q}]
  (let [errors @(rf/subscribe [::errors-for key])
        messages (->> errors (map (comp t* :message)) (join "; "))
        on-blur #(rf/dispatch [::touch! key])
        answer @(rf/subscribe [::answer key])]
    [:div.question
     [:label.field-label {:for (name key)}
      (t key)
      (when (or required? required-without-any?)
        [:span.required " *"])]
     [:div.field
      (case type
        :text
        [:input {:class (when (seq errors) "has-errors")
                 :id (name key)
                 :type :text
                 :value @(rf/subscribe [::answer key])
                 :placeholder (t placeholder)
                 :required required?
                 :on-change #(rf/dispatch [::answer! key (.. % -target -value)])
                 :on-blur on-blur}]

        :email
        [:input {:class (when (seq errors) "has-errors")
                 :type :email
                 :value @(rf/subscribe [::answer key])
                 :placeholder (t placeholder)
                 :required required?
                 :on-change #(rf/dispatch [::answer! key (.. % -target -value)])
                 :on-blur on-blur}]

        :radio
        [:<>
         (map (fn [{:keys [value label]}]
                (let [id (str (name key) "-" value)]
                  ^{:key value}
                  [:span.radio-option {:class (when (seq errors) "has-errors")}
                   [:input {:id id
                            :name (name key)
                            :type :radio
                            :on-blur on-blur
                            :on-change #(rf/dispatch [::answer! key value])
                            :checked (= answer value)}]
                   [:label {:for id} label]]))
              (t options))]

        :select
        [:select {:class (when (seq errors) "has-errors")
                  :value @(rf/subscribe [::answer key])
                  :on-change #(rf/dispatch [::answer! key (.. % -target -value)])
                  :on-blur on-blur}
         (map (fn [opt]
                (let [{:keys [value label]} (->opt opt)]
                  ^{:key value}
                  [:option {:value value} label]))
              (t options))]

        [:span "TODO"])
      (when (seq errors) [:div.error-message messages])]
     (when help [:div.help (t help)])]))

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
  (let [windows @(rf/subscribe [::appointment-windows])
        on-event-click (fn [info]
                         (rf/dispatch [::update-appointment (fc-event->appointment
                                                             (.-event info))])
                         (rf/dispatch [::next-step]))
        lang @(rf/subscribe [::lang])]
    (intake-step
     {:heading
      "Select a time by clicking on one of the available appointment windows"
      :sub-heading :select-appointment-time
      :content
      [:> FullCalendar {:default-view "listWeek"
                        :events windows
                        :eventClick on-event-click
                        :plugins [listPlugin timeGridPlugin]
                        ;; TODO why is "TODAY" text not switching on locale?
                        :locale lang}]})))

(defn- confirmation-details []
  (let [answers @(rf/subscribe [::confirmation-values])]
    [:<> (map (fn [[k v]]
                ^{:key k}
                [:div.detail
                 [:div [:label.field-label @(rf/subscribe [::i18n k])]]
                 [:div v]])
              answers)]))

(defn- confirmation []
  (let [appt @(rf/subscribe [::appointment])]
    (intake-step
     {:sub-heading :confirm-details
      :content
      [:div.intake-step--confirmation
       [confirmation-details]
       [:div.detail
        [:div [:label.field-label (t :appointment-details)]]
        [:div [:b (appointment->str appt)]]]
       [:div.confirm-container
        [:button.call-to-action {:on-click #(rf/dispatch [::confirm!])}
         (t :book-appointment)]]]})))

(defn- confirmed []
  [:div
   [:h3.highlight.spacious (t :appointment-confirmed)]
   [confirmation-details]])


(defn- progress-nav []
  (let [nav-steps @(rf/subscribe [::steps])]
    [:nav
     [:ul.progress
      (doall (map (fn [{:keys [name accessible? current? viewed?] :as step}]
                    (let [nav-title @(rf/subscribe [::i18n name])
                          linkable? (and accessible? (not current?))]
                      ^{:key name}
                      [:li {:class (join " " [(when current? "current") (when viewed? "viewed")])}
                       [:span.nav-link {:class (when-not accessible? "disabled")
                                        :on-click #(when linkable? (rf/dispatch [::update-step step]))}
                        nav-title]]))
                  nav-steps))]]))

(defn intake-ui []
  (let [{:keys [name]} @(rf/subscribe [::current-step])
        lang @(rf/subscribe [::lang])
        lang-options @(rf/subscribe [::lang-options])
        confirmed-info @(rf/subscribe [::confirmed-info])]
    [:div.container.container--get-care {:class (when @(rf/subscribe [::loading?]) "loading")}
     [:header
      [:h1 "Radical Telehealth Collective"]
      [:h2 (t :get-care)]
      (when (not confirmed-info) [progress-nav])]
     [:div.lang-selector
      [:div [:label.field-label {:for "select-language"}
             (t :choose-a-language)]]
      [:select {:value lang
                :id "select-language"
                :on-change #(rf/dispatch [::update-lang (keyword (.. % -target -value))])}
       (map (fn [{:keys [value label]}]
              ^{:key value}
              [:option {:value value} label])
            lang-options)]]
     [:main
      (if confirmed-info
        [confirmed]
        (case name
          :schedule     [schedule]
          :confirmation [confirmation]
          [questions]))]]))


(defn ^:dev/after-load mount! []
  ;; TODO load from GraphQL
  (rf/dispatch [::load-appointment-windows (*generate-calendar-events 100)])
  (dom/render [intake-ui] (.getElementById js/document "rtc-intake-app")))

(defn init! []
  (rf/dispatch-sync [::init-db])
  (mount!))