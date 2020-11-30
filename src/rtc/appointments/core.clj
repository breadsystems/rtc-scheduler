(ns rtc.appointments.core
  (:require
   [clj-time.coerce :as c]
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
                alias
                text-ok
                other-access-needs
                description-of-needs
                preferred-communication-method
                anything-else]} appt
        windows (get-available-windows {:state state})
        pid (first (available-provider-ids windows appt))]
    (if pid
      (appt/create! {:name name
                     :pronouns pronouns
                     :start_time (c/to-sql-time start)
                     :end_time (c/to-sql-time end)
                     :email email
                     :alias alias
                     :ok_to_text (= 1 text-ok)
                       ;; TODO rename db field to :other_access_needs
                     :other_needs other-access-needs
                       ;; TODO add these db fields
                      ;;  :anything_else anything-else
                      ;;  :preferred_communication_method preferred-communication-method
                     :provider_id pid
                     :reason description-of-needs
                     :state state})
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
  (let [note {:appointment_id (:appointment/id note)
              :user_id (:user/id note)
              :note (:note note)
              :date_created (c/to-sql-time (Date.))}]
    (-> (sqlh/insert-into :appointment_notes)
        (sqlh/values [note])
        (sql/format)
        (d/execute!)))
  (let [id (-> ["SELECT id FROM appointment_notes ORDER BY id DESC LIMIT 1"]
               d/query first :id)]
    (assoc note :id id)))

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