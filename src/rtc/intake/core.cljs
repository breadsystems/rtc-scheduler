(ns rtc.intake.core
  (:require
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [clojure.string :refer [join]]
   ["moment" :as moment]
   ["moment/locale/es"]
   ["@fullcalendar/react" :default FullCalendar]
   ["@fullcalendar/timegrid" :default timeGridPlugin]
   ["@fullcalendar/list" :default listPlugin]
   [reagent.dom :as dom]
   [re-frame.core :as rf]
   [rtc.rest.core :as rest]
   [rtc.i18n.core :as i18n]
   [rtc.i18n.data :as i18n-data]
   [rtc.util :refer [->opt]]
   [cognitect.transit :as transit])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))


;; First gather i18n data from flat files.
(def compiled-i18n-data (i18n-data/i18n-data))
;; Compile language options. These won't change over the lifecycle of the app.
(def lang-options (i18n/i18n->lang-options compiled-i18n-data))

(comment
  compiled-i18n-data
  lang-options
  (i18n/best-supported-lang :es compiled-i18n-data)
  (i18n/best-supported-lang :es-MX compiled-i18n-data)
  (i18n/best-supported-lang :en compiled-i18n-data)
  (i18n/best-supported-lang :en-US compiled-i18n-data)
  ;; Careful! If you omit the i18n arg, it'll always default to :en-US!
  (i18n/best-supported-lang :es))



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
   {;; CRSF token comes from the DOM
    :csrf-token csrf-token

    :step 0
    :viewed-up-to-step 0

    :i18n compiled-i18n-data
    :lang-options lang-options
    :lang :en-US

    :loading? false
    :global-error nil

    ;; Where we collect info about the Person Seeking Care.
    :careseeker-info {}
    :appointment-windows []
    :appointment nil
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
     {:name :confirmation}]}))



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
(rf/reg-sub ::global-error :global-error)
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
  @(rf/subscribe [::global-error])
  (rf/dispatch [::global-error :unexpected-error])

  @(rf/subscribe [::can-go-next?])

  @(rf/subscribe [::answers])
  @(rf/subscribe [::answer :name])
  @(rf/subscribe [::answer :text-ok])
  @(rf/subscribe [::answer :preferred-communication-method])
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

  @(rf/subscribe [::appointment-windows])
  (def earliest (moment (:start (first @(rf/subscribe [::appointment-windows])))))
  (.week earliest)
  (.minutes earliest)
  (.format earliest "hh:mm:ss")

  @(rf/subscribe [::lang])
  @(rf/subscribe [::confirmed-info])
  @(rf/subscribe [::appointment])

  ;; current view heading
  (let [{phrase-key :name} @(rf/subscribe [::current-step])]
    @(rf/subscribe [::i18n phrase-key]))

  @(rf/subscribe [::steps])

  ;; Fill out required stuff and jump to schedule step
  (do
    (rf/dispatch [::answer! :state "WA"])
    (rf/dispatch [::answer! :email "coby@tamayo.email"])
    (rf/dispatch [::answer! :description-of-needs "Life is pain"])
    (rf/dispatch [::update-step {:name :confirmation :step 4}]))

  ;; Fill out all fields and jump to schedule step
  (do
    (rf/dispatch [::answer! :name "Coby"])
    (rf/dispatch [::answer! :pronouns "he/him"])
    (rf/dispatch [::answer! :state "WA"])
    (rf/dispatch [::answer! :email "coby@tamayo.email"])
    (rf/dispatch [::answer! :phone "253 555 1234"])
    (rf/dispatch [::answer! :text-ok 1])
    (rf/dispatch [::answer! :preferred-communication-method "phone"])
    (rf/dispatch [::answer! :interpreter-lang "Amharic"])
    (rf/dispatch [::answer! :other-access-needs "Other"])
    (rf/dispatch [::answer! :description-of-needs "Life is pain"])
    (rf/dispatch [::answer! :anything-else "Nah"])
    (rf/dispatch [::update-step {:name :confirmation :step 4}]))

  (rf/dispatch [::confirm!]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;    Re-frame Events    ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn update-answer
  "Update the user's answer to field k"
  [db [_ k v]]
  (assoc-in db [:answers k] v))

(defn update-step
  "Update to an arbirary intake step"
  [db [_ {:keys [step]}]]
  (assoc db :step step))

(defn next-step
"Go to the next intake step"
  [{:keys [step steps viewed-up-to-step] :as db}]
  (let [new-step (min (inc step) (dec (count steps)))]
    (assoc db
           :step new-step
           :viewed-up-to-step (max viewed-up-to-step new-step))))

(defn prev-step
  "Go to the previous intake step"
  [{:keys [step] :as db}]
  (let [new-step (max (dec step) 0)]
    (assoc db :step new-step)))

(defn touch
  "Mark field k as touched (subject to validation)"
  [db [_ k]]
  (update db :touched conj k))


(defn update-appointment [db [_ appt]]
  (assoc db :appointment appt))


;; When the user interacts with the progress nav
(rf/reg-event-fx
 ::update-step
 (fn [{:keys [db]} params]
   (let [updated (update-step db params)]
     {:db updated
      :dispatch [::on-step (:name (current-step updated))]})))

;; When the user hits the back/next buttons
(rf/reg-event-fx
 ::prev-step
 (fn [{:keys [db]}]
   {:db (prev-step db)}))

(rf/reg-event-fx
 ::next-step
 (fn [{:keys [db]}]
   (let [updated (next-step db)]
     {:db updated
      :dispatch [::on-step (:name (current-step updated))]})))

;; When the user focuses a field for the first time. For validation purposes.
(rf/reg-event-db ::touch! touch)
;; When the user answers a question
(rf/reg-event-db ::answer! update-answer)
;; When the user selects an appointment time
(rf/reg-event-db ::update-appointment update-appointment)

(rf/reg-event-fx ::update-lang (fn [{:keys [db]} [_ lang]]
                                 {:db (assoc db :lang lang)
                                  :moment-locale lang}))

(rf/reg-fx :moment-locale (fn [lang]
                            (.locale moment (name lang))))

(defn- focus-relevant-elem
  "For good keyboard UX, focus on the input for the first question. Should run whenever the question changes.
   Can override by setting the data-keep-focus attribute, which keeps the focus on the currently focused element
   if this attribute is true."
  []
  (when-not (.. js/document.activeElement -dataset -keepFocus)
    (when-let [elem (.querySelector js/document ".field input, .field select")]
      (.focus elem))))

(rf/reg-event-fx
 ::on-step
 (fn [{:keys [db]} [_ step-name]]
   (when (= :schedule step-name)
     {:db (assoc db :loading? true)
      ;; Make a network request for the available appointment windows.
      ::fetch-appointment-windows [(:answers db)]})))

;; Called when we get an error from the REST API, or some other event we need
;; to alert the user to.
(rf/reg-event-db
 ::global-error
 (fn [db [_ err]]
   (assoc db :loading? false :global-error err)))

;; Called when we get the appointment windows back from the server.
(rf/reg-event-db
 ::load-appointment-windows
 (fn [db [_ windows]]
   (assoc db :loading? false :appointment-windows (:data windows))))

(rf/reg-fx
 ::fetch-appointment-windows
 (fn [[answers]]
   (rest/get! "/api/v1/windows"
              {:query-params {:state (:state answers)}}
              ::load-appointment-windows
              #(rf/dispatch [::global-error :unexpected-error]))))

(rf/reg-fx
 ::book-appointment!
 (fn [params]
   (rest/post! "/api/v1/appointment"
               {:transit-params params
                :headers {"x-csrf-token" (:csrf-token params)}}
               ::appointment-response
               #(rf/dispatch [::global-error :unexpected-error]))))

(rf/reg-event-fx
 ::confirm!
 (fn [{:keys [db]}]
   (let [{:keys [csrf-token answers appointment loading? confirmed-info]} db
         should-mutate? (and (not loading?) (not confirmed-info))]
     ;; Dispatching this event when the UI is already loading or an appointment
     ;; has already been confirmed is a noop.
     (when should-mutate?
       {:db (assoc db :loading? true)
        ::book-appointment! (merge {:csrf-token csrf-token} answers appointment)}))))

(defn process-appointment-response [db [_ {:keys [success data errors]}]]
  (if success
    (assoc db
           :loading? false
           :confirmed-info (:appointment data))
    (assoc (prev-step db)
           :loading? false
           :global-error (or (:reason (first errors)) :unexpected-error)
           :appointment-windows (or (:windows data)
                                    (:appointment-windows db)))))

(rf/reg-event-db ::appointment-response process-appointment-response)

(comment
  ;; Moment.js experiments
  (moment)
  (.format (moment) "YYYY-MM-DD")
  (.format (moment))
  (.format (doto (moment)
             (.add (rand-int 10) "days")
             (.add (rand-int 4) "hours"))
           "dddd, MMMM Do")

  (rf/dispatch [::update-step {:name :basic-info :step 0}])
  (rf/dispatch [::update-step {:name :contact-info :step 1}])
  (rf/dispatch [::update-step {:name :schedule :step 4}])
  (rf/dispatch [::update-step {:name :confirmation :step 5}])

  (rf/dispatch [::update-lang :en])
  (rf/dispatch [::update-lang :es])
  ;; Esperanto
  (rf/dispatch [::update-lang :eo]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;        Helpers        ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn t [k]
  (or @(rf/subscribe [::i18n k]) ""))

(defn t* [ks]
  (join " " (map t ks)))

(defn appointment->str [{:keys [start end lang]}]
  (let [s (moment start (name lang))
        e (moment end (name lang))]
    (str (.format s "h:mma - ") (.format e "h:mma ") (.format s "dddd, MMM Do"))))

(defn- on-enter
  "Given a callback f, returns a function that takes a KeyboardEvent and calls f
   IFF the event is an Enter keypress, or otherwise does nothing."
  [f]
  (fn [e]
    (when (= 13 (or (.-code e) (.-which e)))
      (f))))

(comment
  (t :name)
  (t :unexpected-error)
  (t :no-appointments-this-week)
  (i18n-data/i18n-data)
  (t* [:please-enter :name])
  (map (comp t* :message) @(rf/subscribe [::errors-for :state]))

  (appointment->str {:start #inst "2020-07-06T16:00:00"
                     :end #inst "2020-07-06T16:30:00"
                     :lang :en_US}))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;      Components       ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- intake-step [{:keys [sub-heading content]}]
  (let [show-next? (not @(rf/subscribe [::last-step?]))
        error @(rf/subscribe [::global-error])]
    [:section
     [:header
      (when sub-heading [:h3 (t sub-heading)])]
     (when error [:h3.error-message (t error)])
     [:div
      content]
     [:footer.intake-footer
      [:button.prev {:on-click #(rf/dispatch [::prev-step])
                     :disabled (not @(rf/subscribe [::can-go-prev?]))} (t :back)]
      (when show-next?
        [:button.next {:on-click #(rf/dispatch [::next-step])
                       :disabled (not @(rf/subscribe [::can-go-next?]))} (t :next)])]]))

(defn- errors-class [errors]
  (when (seq errors) "has-errors"))

(defmulti ^:private question-input :type)

;; No default: force a compiler error.

(defmethod question-input :text
  [{:keys [key type errors placeholder required? on-blur]}]
  [:input {:class (errors-class errors)
           :id (name key)
           :type :text
           :value @(rf/subscribe [::answer key])
           :placeholder (t placeholder)
           :required required?
           :on-change #(rf/dispatch [::answer! key (.. % -target -value)])
           :on-blur on-blur}])

(defmethod question-input :email
  [{:keys [key errors placeholder required? on-blur]}]
  [:input {:class (errors-class errors)
           :type :email
           :value @(rf/subscribe [::answer key])
           :placeholder (t placeholder)
           :required required?
           :on-change #(rf/dispatch [::answer! key (.. % -target -value)])
           :on-blur on-blur}])

(defmethod question-input :radio
  [{:keys [key errors required? on-blur options]}]
  (let [answer @(rf/subscribe [::answer key])]
    [:<>
     (map (fn [{:keys [value label]}]
            (let [id (str (name key) "-" value)]
              ^{:key value}
              [:span.radio-option {:class (errors-class errors)}
               [:input {:id id
                        :name (name key)
                        :type :radio
                        :on-blur on-blur
                        :on-change #(rf/dispatch [::answer! key value])
                        :checked (= answer value)}]
               [:label {:for id} label]]))
          (t options))]))

(defmethod question-input :select
  [{:keys [key errors on-blur options]}]
  [:select {:class (errors-class errors)
                  :value @(rf/subscribe [::answer key])
                  :on-change #(rf/dispatch [::answer! key (.. % -target -value)])
                  :on-blur on-blur}
         (map (fn [opt]
                (let [{:keys [value label]} (->opt opt)]
                  ^{:key value}
                  [:option {:value value} label]))
              (t options))])

(defn- question [{:keys [key help required? required-without-any? placeholder options] :as q}]
  (let [errors @(rf/subscribe [::errors-for key])
        messages (->> errors (map (comp t* :message)) (join "; "))
        on-blur #(rf/dispatch [::touch! key])]
    [:div.question
     [:label.field-label {:for (name key)}
      (t key)
      (when (or required? required-without-any?)
        [:span.required " *"])]
     [:div.field
      (question-input (merge q {:errors errors
                                :on-blur on-blur}))
      (when (seq errors) [:div.error-message messages])]
     (when help [:div.help (t help)])]))

;; The focus MAY change when the question changes, so use the React lifecycle
;; to trigger that potential change.
(def ^:private stateful-question
  (with-meta question
    {:component-did-mount focus-relevant-elem}))

(defn- questions []
  (let [step (:name @(rf/subscribe [::current-step]))
        qs @(rf/subscribe [::questions step])]
    [intake-step
     {:heading (t step)
      :content (map (fn [q]
                      ^{:key (:key q)}
                      [stateful-question q])
                    qs)}]))

(defn- schedule []
  (let [windows @(rf/subscribe [::appointment-windows])
        earliest (moment (:start (first windows)))
        on-event-click (fn [info]
                         (rf/dispatch [::update-appointment {:start (.. info -event -start)
                                                             :end (.. info -event -end)}])
                         (rf/dispatch [::next-step]))
        lang @(rf/subscribe [::lang])]
    (intake-step
     {:heading
      "Select a time by clicking on one of the available appointment windows"
      :sub-heading :select-appointment-time
      :content
      [:> FullCalendar {:initial-view "listWeek"
                        :events windows
                        :eventClick on-event-click
                        :plugins [listPlugin timeGridPlugin]
                        :scrollTime (.format earliest "hh:mm:00")
                        :noEventsContent #(t :no-appointments-this-week)
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
  (let [appt @(rf/subscribe [::appointment])
        lang @(rf/subscribe [::lang])]
    (intake-step
     {:sub-heading :confirm-details
      :content
      [:div.intake-step--confirmation
       [confirmation-details]
       [:div.detail
        [:div [:label.field-label (t :appointment-time)]]
        [:div [:b (appointment->str (assoc appt :lang lang))]]]
       [:div.confirm-container
        [:button.call-to-action {:on-click #(rf/dispatch [::confirm!])}
         (t :book-appointment)]]]})))

(comment
  (deref (rf/subscribe [::appointment]))
  (deref (rf/subscribe [::confirmed-info])))

(defn- confirmed []
  (let [appt @(rf/subscribe [::appointment])
        {:keys [provider_first_name provider_last_name start_time end_time]} @(rf/subscribe [::confirmed-info])
        provider-name (str provider_first_name " " provider_last_name)
        lang @(rf/subscribe [::lang])]
    [:div
     [:h3.highlight.spacious (t :appointment-confirmed)]
     [:p.help.spacious (t :we-will-follow-up)]
     [:div.detail
      [:div [:label.field-label (t :appointment-time)]]
      [:div (appointment->str (assoc appt :lang lang))]]
     [:div.detail
      [:div [:label.field-label (t :provider-name)]]
      [:div provider-name]]]))


(defn- progress-nav []
  (let [nav-steps @(rf/subscribe [::steps])]
    [:nav
     [:ul.progress
      (doall (map (fn [{:keys [name accessible? current? viewed?] :as step}]
                    (let [nav-title @(rf/subscribe [::i18n name])
                          linkable? (and accessible? (not current?))]
                      ^{:key name}
                      [:li {:class (join " " [(when current? "current") (when viewed? "viewed")])}
                       [:a.nav-link {:class (when-not accessible? "disabled")
                                     :tab-index (if accessible? 0 -1)
                                     :on-click #(when linkable? (rf/dispatch [::update-step step]))
                                     :on-key-press (on-enter #(when linkable? (rf/dispatch [::update-step step])))
                                     ;; Keep the focus on this element when the question changes,
                                     ;; rather than focus on the first question/field as usual.
                                     :data-keep-focus true}
                        nav-title]]))
                  nav-steps))]]))

(defn intake-ui []
  (let [{:keys [name]} @(rf/subscribe [::current-step])
        lang @(rf/subscribe [::lang])
        lang-options @(rf/subscribe [::lang-options])
        confirmed-info @(rf/subscribe [::confirmed-info])]
    [:div.container.container--get-care {:class (when @(rf/subscribe [::loading?]) "loading")}
     [:aside.lang-selector
      [:label.field-label {:for "select-language"}
       (t :choose-a-language)]
      [:select {:value lang
                :id "select-language"
                :on-change #(rf/dispatch [::update-lang (keyword (.. % -target -value))])}
       (map (fn [{:keys [value label]}]
              ^{:key value}
              [:option {:value value} label])
            lang-options)]]
     [:header
      [:h1 [:a {:href "/"} "Radical Telehealth Collective"]]
      [:h2 (t :get-care)]
      (when (not confirmed-info) [progress-nav])]
     [:main
      (if confirmed-info
        [confirmed]
        (case name
          :schedule     [schedule]
          :confirmation [confirmation]
          [questions]))]]))


(defn ^:dev/after-load mount! []
  (dom/render [intake-ui] (.getElementById js/document "rtc-intake-app")))

(defn init! []
  (let [token (.-content (js/document.querySelector "meta[name=csrf-token]"))]
    (rf/dispatch-sync [::init-db {:csrf-token token}]))
  (let [browser-lang (or (.-language js/navigator)
                         (.-userLanguage js/navigator))
        supported-lang (i18n/best-supported-lang browser-lang
                                                 compiled-i18n-data)]
    (js/console.log "Detected language:" browser-lang "Selected language:" (name supported-lang))
    (rf/dispatch [::update-lang supported-lang]))
  (mount!))

(comment

  (go (let [response (<! (http/post
                          "/api/v1/appointment"
                          {:form-params {:name "Me" :date #inst "2020-01-01T00:00:00-00:00"}}))
            reader (transit/reader :json)]
        (js/console.clear)
        (prn (transit/read reader (:body response)))))

  ;;
  )
