(ns rtc.appointments.windows
  (:require
   [clojure.set :refer [union]]
   [clojure.spec.alpha :as spec]
   [rtc.appointments.internal.timeframes :as t]))



(spec/def ::date (spec/inst-in #inst "2000" #inst "2100"))

(spec/def ::drange (spec/and (spec/coll-of ::date :kind vector? :count 2 :distinct true)
                             (fn [[start end]]
                               ;; duration >= 30 minutes
                               (>= (- (inst-ms end) (inst-ms start)) 1800000))))

;; A window should be between 10m and 2h
(spec/def ::window-minutes (spec/and int? #(> % 10) #(> 120 %)))

(spec/def ::provider-id pos-int?)
(spec/def ::start ::date)
(spec/def ::end ::date)

(spec/def ::window (spec/keys :req-un [::provider-id ::start ::end]))
(spec/def ::windows (spec/coll-of ::window :kind vector?))

(spec/def ::provider-pool (spec/coll-of ::provider-id :kind set? :min-count 1))



(defn availabilities->windows
  "The core algorithm of the appointment calendar. Takes a list of availabilities,
   a list of appointments, a from/to date, and a window length (in minutes), and
   returns a vector of availability windows of the form:
   {:start #inst ...
    :end #inst ...
    :provider-id 123}"
  [avails appts from to w]
  {:pre [(spec/valid? ::windows avails)
         (spec/valid? ::windows appts)
         (t/before? from to)
         (spec/valid? ::window-minutes w)]}

  (let [;; For each availability, compute the Timeframe that overlaps
        ;; with [from to] (if any).
        timeframes (filter some? (map #(t/avail->timeframe % from to) avails))

        ;; For each Timeframe [A'n B'n], break into chunks of length w
        ;; [[A'0 A'w] [A'w A'2w] ,,, [A'n-w A'n]]
        ;; Record in a map where the keys are vectors [A'n A'n+w]
        ;; and the values are maps with Provider data.
        window->provider-id-mappings (map #(t/timeframe->window-map % w) timeframes)

        ;; Merge all Timeframe maps together, conjoining (unique) Provider
        ;; values into sets. We now have a mapping of window start/end times
        ;; to *potentially* available providers (we just need to take
        ;; existing appointments into account).
        window->provider-ids (reduce (fn [w->ps w->p]
                                       ;; Cast provider ids into single-element sets,
                                       ;; and merge all the sets together by window.
                                       (merge-with union w->ps (into {} (map (fn [[w p]]
                                                                               [w #{p}])
                                                                             w->p))))
                                     {}
                                     window->provider-id-mappings)

        ;; For each Appoinment, round start (down) and end (up) to nearest
        ;; window edge, i.e. (+ from (* n w)), where n is an int, to get
        ;; [start'r end'r]. Remove the Provider from the set indexed by
        ;; [start'r end'r].
        windows-minus-appts (reduce (fn [w->p {:keys [provider-id] :as appt}]
                                      ;; For each appointment, reduce over the
                                      ;; discrete overlapping windows and remove
                                      ;; the corresponding provider's availability
                                      ;; during that window.
                                      (reduce (fn [w->p window]
                                                (println window)
                                                (update w->p window disj provider-id))
                                              w->p
                                              (t/appt->windows appt from w)))
                                    window->provider-ids
                                    appts)

        ;; The Windows remaining with at least one Provider are our
        ;; Available Windows. Collapse keys/vals into a single vector of maps.
        ;; (println windows-minus-appts)
        available-windows (reduce (fn [windows [[start end] provider-ids]]
                                    (if (seq provider-ids)
                                      (conj windows {:start start :end end :provider-ids provider-ids})
                                      windows))
                                  []
                                  windows-minus-appts)]
    ;; Sort windows by start date
    (sort-by (comp inst-ms :start) available-windows)))


(comment
  (let [avails [{:start #inst "2020-01-03T11:00"
                 :end   #inst "2020-01-03T14:00"
                 :provider-id 1}
                {:start #inst "2020-01-03T12:30"
                 :end   #inst "2020-01-03T15:00"
                 :provider-id 2}]
        ]))