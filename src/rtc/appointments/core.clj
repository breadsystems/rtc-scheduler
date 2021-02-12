(ns rtc.appointments.core
  (:require
   [clj-time.coerce :as c]
   [clojure.set :refer [rename-keys]]
   [honeysql.core :as sql]
   [honeysql.helpers :as sqlh]
   [rtc.appointments.appointments :as appt]
   [rtc.appointments.availabilities :as avail]
   [rtc.appointments.states :as st]
   [rtc.appointments.windows :as w]
   [rtc.db :as d]
   [rtc.util :as util])
  (:import
   [java.text SimpleDateFormat]
   [java.util Date]))


(defonce ONE-DAY-MS (* 24 60 60 1000))

;; Define our window length to be half an hour
(defonce WINDOW-MS (* 30 60 1000))

(defn- window-range []
  (let [;; TODO tighten up this logic for more accurate availability
        ;; Look for availabilities starting this time five days from now
        from (+ (inst-ms (util/midnight-this-morning)) (* 5 ONE-DAY-MS))
        to (+ from (* 28 ONE-DAY-MS))]
    [from to]))

(defn params->windows [{:keys [from to state]}]
  (let [states (get st/state-mappings state)
        avails (avail/get-availabilities {:from from :to to :states states})
        appts (appt/get-appointments {:from from :to to :states states})]
    (map w/format-window
         (w/->windows (map w/coerce avails) (map w/coerce appts) from to WINDOW-MS))))

(defn get-available-windows [params]
  (let [[from to] (window-range)
        state (get params "state")]
    (params->windows {:from from :to to :state state})))

(def ^:private fmt (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss"))

(defn- flatten-formatted [{:keys [start end]}]
  [(.format fmt start) (.format fmt end)])

(defn- available-provider-ids [windows appt]
  (get (into {} (map (fn [w]
                       [(flatten-formatted w) (:ids w)])
                     windows))
       (flatten-formatted appt)))

(defn book-appointment! [appt]
  (let [{:keys [name
                pronouns
                state
                start
                end
                email
                phone
                alias
                text-ok
                interpreter-lang
                other-access-needs
                description-of-needs
                preferred-communication-method
                anything-else]} appt
        windows (get-available-windows {:state state})
        pid (first (available-provider-ids windows appt))]
    (if pid
      (let [{:keys [id] :as booked-appt}
            (appt/create! {:name name
                           :pronouns pronouns
                           :start_time (c/to-sql-time start)
                           :end_time (c/to-sql-time end)
                           :email email
                           :phone phone
                           :alias alias
                           :ok_to_text (= 1 text-ok)
                           :other_notes anything-else
                           :preferred_communication_method preferred-communication-method
                           :provider_id pid
                           :reason description-of-needs
                           :state state})]
        (when (seq other-access-needs)
          (d/create-appointment-need! {:appointment/id id
                                       :need/id "other"
                                       :info other-access-needs}))
        (when (seq interpreter-lang)
          (d/create-appointment-need! {:appointment/id id
                                       :need/id "interpretation"
                                       :info interpreter-lang}))
        booked-appt)
      ;; TODO insert access needs
      (throw (ex-info "Appointment window is unavailable!" {:windows windows
                                                            :reason :window-unavailable})))))

(defn schedule-availability! [avail]
  (let [avail {:start_time (:start avail)
               :end_time (:end avail)
               :provider_id (:user/id avail)}
        existing (avail/get-overlapping avail)]
    (if (seq existing)
      (throw (ex-info "Availability overlaps with an existing one!"
                      {:reason :overlaps-existing
                       :availabilities existing}))
      (avail/create! avail))))

(defn update-availability! [avail]
  (let [avail {:id (:id avail)
               :provider_id (:user/id avail)
               :start_time (:start avail)
               :end_time (:end avail)}
        ;; Look for any other existing availabilities that
        ;; overlap with this one.
        conflicting (filter
                     #(not= (:id avail) (:id %))
                     (avail/get-overlapping avail))]
    (if (seq conflicting)
        (throw (ex-info "Availability overlaps with an existing one!"
                        {:reason :overlaps-existing
                         :availabilities conflicting}))
        (avail/update! avail))))

(defn delete-availability! [avail]
  (avail/delete! (:id avail))
  avail)

(defn create-note! [note]
  (let [note (assoc note :date_created (c/to-sql-time (Date.)))]
    (-> (sqlh/insert-into :appointment_notes)
        (sqlh/values [(rename-keys note {:appointment/id :appointment_id
                                         :user/id :user_id})])
        (sql/format)
        (d/execute!))
    (let [id (-> ["SELECT id FROM appointment_notes ORDER BY id DESC LIMIT 1"]
                 d/query first :id)]
      (assoc note :id id))))

(defn fulfill-need! [{:need/keys [id fulfilled?]
                      appt-id :appointment/id
                      :as need}]
  {:pre [(boolean? fulfilled?) (boolean appt-id) (boolean id)]}
  (-> (sqlh/update :appointment_needs)
      (sqlh/sset {:fulfilled fulfilled?
                  :confirmed_at (c/to-sql-time (Date.))})
      (sqlh/where [:and
                   [:= :appointment_id (Integer. appt-id)]
                   [:= :need_id (name id)]])
      (sql/format)
      (d/execute!))
  need)

(defn details [id]
  (let [id (Integer. id)
        appt (-> (sqlh/select :id :email :phone :name :pronouns :alias :state
                              [:start_time :start] [:end_time :end] :reason
                              :other_access_needs :other_access_needs_met
                              :other_notes :ok_to_text)
                 (sqlh/from :appointments)
                 (sqlh/where [:= :id (Integer. id)])
                 (sql/format)
                 (d/query)
                 first)
        notes (-> (sqlh/select :note :user_id :date_created)
                  (sqlh/from [:appointment_notes :n])
                  (sqlh/where [:= :n.appointment_id id])
                  (sqlh/order-by [[:date_created :desc]])
                  (sql/format)
                  (d/query)
                  vec)
        ;; format notes & access needs
        notes (map #(rename-keys % {:user_id :user/id}) notes)
        access-needs (appt/id->needs id)]
    (-> appt
        (assoc :notes notes)
        (assoc :access-needs access-needs))))

(comment

  (create-note! {:appointment/id 2
                 :user/id 1
                 :note "Lorem ipsum dolor sit amet."})

  (def now (java.util.Date.))
  (w/->windows [] [] (inst-ms now) (+ 360000000 (inst-ms now)) WINDOW-MS)

  (book-appointment! {:start #inst "2020-11-29T11:30-08:00"
                      :end #inst "2020-11-29T12:00-08:00"
                      :reason "My everything hurts"
                      :state "WA"}))
