(ns rtc.intake.core
  (:require
   [clojure.string :refer [join]]
   ["moment" :as moment]
   ["@fullcalendar/react" :default FullCalendar]
   ["@fullcalendar/timegrid" :default timeGridPlugin]
   ["@fullcalendar/list" :default listPlugin]
   [reagent.dom :as dom]
   [re-frame.core :as rf]
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
    :viewed-up-to-step 0
    :lang :en
    :lang-options [{:value :en :label "English"}
                   {:value :es :label "Español"}]
    ;; Where we collect info about the Person Seeking Care.
    :careseeker-info {}
    :appointment-windows []
    :answers {}
    :touched #{}
    ;; TODO farm this out to an EDN file to make it more configurable?
    :steps
    [{:name :basic-info
      :questions
      [{:key :name
        :help :name-help
        :placeholder :anonymous
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
     {:name :confirm}]
    :i18n
    ;; TODO how to deal with groupings like this?
    {:en {:yes-no [{:value 1 :label "Yes"}
                   {:value 0 :label "No"}]
          :communication-methods [{:value "phone" :label "Phone"}
                                  {:value "email" :label "Email"}]
          :basic-info "Basic Info"
          :contact-info "Contact Info"
          :access-needs "Access Needs"
          :medical-needs "Medical Needs"
          :schedule "Schedule an Appointment"
          :confirm "Confirm"
          :name "Name"
          :name-help "Not required."
          :anonymous "Anonymous"
          :pronouns "Pronouns"
          :they-them "they/them/theirs"
          :state "State"
          :state-help "Needed to find a provider who can legally provide care for you."
          :email "Email"
          :phone "Phone"
          :email-or-phone "Please enter your Email, Phone, or both."
          :email-or-phone-help "Email or Phone is required."
          :text-ok "OK to text?"
          :text-ok-help "Depending on your carrier, you may incur charges."
          :preferred-communication-method "Preferred Communcation Method"
          :interpreter-lang "Do you need an interpreter?"
          :interpreter-lang-help "Let us know which language you are most comfortable speaking. If you speak English, leave this blank."
          :interpreter-options [{:value nil :label "Choose..."}
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
          :please-describe-medical-needs "Please briefly describe your medical needs."
          :anything-else "Anything we forgot to ask?"
          :please-enter "Please enter your"
          :states
          [{:value ""   :label "Choose a state"}
           {:value "WA" :label "Washington"}
           {:value "NY" :label "New York"}
           {:value "CA" :label "California"}]}
     ;; Sorry, Ramsey!!
     :es {:yes-no [{:value 1 :label "Si"}
                   {:value 0 :label "No"}]
          :communication-methods [{:value "phone" :label "Teléfono"}
                                  {:value "email" :label "Dirección de Correo Electrónica"}]
          :basic-info "TODO Basic Info"
          :contact-info "TODO Contact Info"
          :access-needs "TODO Access Needs"
          :medical-needs "TODO Medical Needs"
          :name "Nombre"
          :anonymous "Anónimo"
          :pronouns "Pronombres"
          :they-them "elles/elle"
          :state "Estado"
          :state-help "Se requiere para que le conectamos a un proveedor de atención médica que pueda proporcionarle servicios de telemedicina legalmente"
          :email "Dirección de Correo Electrónica"
          :phone "Teléfono"
          :email-or-phone "TODO Please enter your Email, Phone, or both."
          :email-or-phone-help "TODO Email or Phone is required."
          :text-ok "¿Está bien enviar un mensaje de texto?"
          :text-ok-help "Le pedimos esto ya que dependiendo de su operador puede incurrir en cargos."
          :preferred-communication-method "TODO Preferred Communcation Method"
          :interpreter-lang "¿Necesitas un intérprete?"
          :interpreter-lang-help "Seleccione un idioma o proporcione como \"otro\" a continuación"
          :interpreter-options [{:value nil :label "Choose..."}
                                "Amárico"
                                "Arabe"
                                "Lenguaje de Señas - Americano"
                                "Chino Cantonés"
                                "Chino Mandarín"
                                "Khmer"
                                "Coreano"
                                "Punjabi"
                                "Russo"
                                "Español"
                                "Somali"
                                "Tagalo"
                                "Ucranio"
                                "Vietnamita"
                                {:value :other :label "Otro..."}]
          :other-access-needs "¿Alguna otra necesidad de acceso que pueda ayudarnos a apoyarlo mejor?"
          :description-of-needs "Descripción breve de la necesidad médica"
          :description-of-needs-help "Por ejemplo, fiebre y síntomas de dolor de garganta durante 3 días, o necesita una receta para insulina, hormonas"
          :please-describe-medical-needs "TODO please briefly describe your medical needs"
          :anything-else "¿Algo que olvidamos preguntar?"
          :please-enter "TODO Please enter your"
          :schedule "TODO Schedule an Appointment"
          :states
          [{:value ""   :label "Choose a state"}
           {:value "WA" :label "Washington"}
           {:value "NY" :label "New York"}
           {:value "CA" :label "California"}]}}}))



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

(defn touch [db [_ k]]
  (update db :touched conj k))

(defn step-valid? [{:keys [answers] :as db}]
  ;; Each validator returns a list of errors. Check that each list is empty.
  (empty? (questions->errors (current-questions db) answers)))

(defn can-go-prev? [{:keys [step]}]
  (> step 0))

(defn can-go-next? [{:keys [step steps] :as db}]
  (and
   (> (count steps) (inc step))
   (step-valid? db)))

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

(rf/reg-sub ::steps accessible-steps)
(rf/reg-sub ::appointment-windows :appointment-windows)
(rf/reg-sub ::current-step current-step)
(rf/reg-sub ::questions current-questions)
(rf/reg-sub ::can-go-prev? can-go-prev?)
(rf/reg-sub ::can-go-next? can-go-next?)
(rf/reg-sub ::errors current-errors)
(rf/reg-sub ::errors-for errors-for)
(rf/reg-sub ::touched :touched)
(rf/reg-sub ::answers :answers)
(rf/reg-sub ::answer answer)
(rf/reg-sub ::i18n (fn [db [_ phrase-key]]
                     (i18n/t db phrase-key)))
(rf/reg-sub ::lang :lang)
(rf/reg-sub ::lang-options :lang-options)

(comment
  @(rf/dispatch [::init-db])
  @(rf/subscribe [::steps])
  @(rf/subscribe [::questions])
  @(rf/subscribe [::current-step])
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

  @(rf/subscribe [::lang])

  ;; current view heading
  (let [{phrase-key :name} @(rf/subscribe [::current-step])]
    @(rf/subscribe [::i18n phrase-key]))

  @(rf/subscribe [::steps]))



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


(rf/reg-event-db ::update-step update-step)

(rf/reg-event-db ::answer! update-answer)
(rf/reg-event-db ::prev-step prev-step)
(rf/reg-event-db ::next-step next-step)
(rf/reg-event-db ::touch! touch)

(rf/reg-event-db ::update-lang (fn [db [_ lang]]
                                 (assoc db :lang lang)))

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

  (rf/dispatch [::update-step {:name :basic-info :step 0}])
  (rf/dispatch [::update-step {:name :contact-info :step 1}])
  (rf/dispatch [::update-step {:name :schedule :step 4}])
  
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

(comment
  (t :name)
  (t* [:please-enter :name])
  (map (comp t* :message) @(rf/subscribe [::errors-for :state])))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;      Components       ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- intake-step [{:keys [heading sub-heading content]}]
  [:section
   [:header
    (when sub-heading [:h4 sub-heading])]
   [:div
    content]
   [:footer.intake-footer
    [:button.prev {:on-click #(rf/dispatch [::prev-step])
                   :disabled (not @(rf/subscribe [::can-go-prev?]))} "Back"]
    [:button.next {:on-click #(rf/dispatch [::next-step])
                   :disabled (not @(rf/subscribe [::can-go-next?]))} "Next"]]])

(defn- question [{:keys [key type help required? required-without-any? placeholder options] :as q}]
  (let [errors @(rf/subscribe [::errors-for key])
        messages (->> errors (map (comp t* :message)) (join "; "))
        error-class (when (seq errors) "has-errors")
        on-blur #(rf/dispatch [::touch! key])]
    [:div.question
     [:label.field-label
      (t key)
      (when (or required? required-without-any?)
        [:span.required " *"])]
     [:div.field
      (case type
        :text
        [:input {:class (when (seq errors) "has-errors")
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
                   [:input {:id id :name (name key) :type :radio :on-blur on-blur}]
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
  (let [windows @(rf/subscribe [::appointment-windows])]
    (intake-step
     {:heading
      "Select a time by clicking on one of the available appointment windows"
      :sub-heading
      "These are the appointment times available for residents of your state."
      :content
      [:> FullCalendar {:default-view "listWeek"
                        :events windows
                        :eventClick (fn [info] (js/console.log info))
                        :plugins [listPlugin timeGridPlugin]}]})))


(defn- progress-nav []
  (let [nav-steps @(rf/subscribe [::steps])]
    [:nav
     [:ul.progress
      (map (fn [{:keys [name accessible? current? viewed?] :as step}]
             (let [nav-title (t name)
                   linkable? (and accessible? (not current?))]
               ^{:key name}
               [:li {:class (join " " [(when current? "current") (when viewed? "viewed")])}
                [:span.step-link {:class (when-not accessible? "disabled")
                                  :on-click #(when linkable? (rf/dispatch [::update-step step]))}
                 nav-title]]))
           nav-steps)]]))

(defn intake-ui []
  (let [{:keys [name]} @(rf/subscribe [::current-step])
        lang @(rf/subscribe [::lang])
        lang-options @(rf/subscribe [::lang-options])]
    [:div.container.container--get-care
     [:header
      [:h1 "Radical Telehealth Collective"]
      [:h2 "Get Care"]
      [progress-nav]]
     [:main
      (if (= :schedule name)
        [schedule]
        [questions])]
     [:div.lang-selector
      [:select {:value lang
                :on-change #(rf/dispatch [::update-lang (keyword (.. % -target -value))])}
       (map (fn [{:keys [value label]}]
              ^{:key value}
              [:option {:value value} label])
            lang-options)]]]))


(defn ^:dev/after-load mount! []
  ;; TODO load from GraphQL
  (rf/dispatch [::load-appointment-windows (*generate-calendar-events 100)])
  (dom/render [intake-ui] (.getElementById js/document "rtc-intake-app")))

(defn init! []
  (rf/dispatch-sync [::init-db])
  (mount!))