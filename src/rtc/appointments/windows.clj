(ns rtc.appointments.windows)


(defn- ->earlier-window-edge
  "Bump start down to the next lowest multiple of w,
   AKA the most recent window edge."
  [start from w]
  (let [start-max (max start from)]
    (- start-max (mod (Math/abs (- start-max from)) w))))

(defn- ->next-window-edge
  "Bump end up to the next highest multiple of w,
   AKA the next window edge."
  [end to w]
  (let [start-min (min end to)]
    (+ start-min (mod (Math/abs (- start-min to)) w))))

(defn avail->ops [from to w {:keys [start end id]}]
  [[(max from (->next-window-edge start to w)) id 1]
   [(min to (->earlier-window-edge end from w)) id -1]])

(defn appt->ops [from to w {:keys [start end id]}]
  [[(->earlier-window-edge start from w) id -1]
   [(->next-window-edge end to w) id 1]])

(defn apply-ops [id->toggle [[_ id opcode] & ops]]
  (if opcode
    (update (apply-ops id->toggle ops) id #(+ (or % 0) opcode))
    id->toggle))

(defn fold-ops [t->ops [start id op]]
  (update t->ops start conj [start id op]))

(defn ->ids [toggles]
  (let [one? #(= 1 %)
        positives (filter (comp one? val) toggles)]
    (into [] (map key positives))))

(defn coerce [m]
  {:id    (:provider_id m)
   :start (inst-ms (:start_time m))
   :end   (inst-ms (:end_time m))})


(defn ->windows [avails appts from to w]
  {:pre [(> to from)
         (>= (- to from) w)
         (pos-int? w)
         ;; TODO validate avails
         ;; TODO validate appts
         ]}

  ;; We can model an availability window as a pair of events:
  ;; a provider becomes available at `start` and then unavailable
  ;; at `end`. We can encode this as a list of facts:
  ;;
  ;; * the provider id
  ;; * the timestamp (start or end)
  ;; * whether the provider is now available or unavailable
  ;;
  ;; Together, these three piece of info are called an operation,
  ;; or op for short. We'll call the third piece - available vs.
  ;; unavailable, the opcode. The opcode can be represented as a
  ;; 1 for available or -1 for unavailable.
  ;; 
  ;; There are a few nice properties of this approach:
  ;; 
  ;; * For a given start time, it doesn't matter which order ops
  ;;   are applied in. If an appointment op comes before an
  ;;   availability op, the value for that resource temporarily
  ;;   goes down to -1. Once we parse the availability op, it goes
  ;;   back up to 0. The effect is as you'd expect: the provider
  ;;   has an availability window, but they are booked during that
  ;;   time and so are not available for booking a new appointment
  ;;   during that window.
  ;; * If an availability happens to span over a window edge but
  ;;   does NOT span an entire whole window, it "collapses" onto
  ;;   the window edge it surrounds, effectively turning it into
  ;;   a noop. This is what we want, because if a provider has stated
  ;;   they're available during a given time, but they're not
  ;;   actually available for a full appointment slot, we don't
  ;;   want to say they are.
  ;; * If an availability happens to span less than one window,
  ;;   its start/end times "collapse" onto the opposite edges.
  ;;   For example, if an availability starts at 1:05pm and ends
  ;;   at 1:25pm, with a 30-minute window time we would see that
  ;;   availability collapse onto 1:30 - 1:00pm! No, that's not a
  ;;   typo: the 1:05 collapses UP to 1:30, while the 1:25 collapses
  ;;   DOWN to 1:00. So the "end" - the op that says this provider
  ;;   is no longer available - comes before the start. This is weird
  ;;   and counterintuitive, but it's also what we want because it
  ;;   means we won't disingenuously say that provider is available
  ;;   from 1:00 to 1:30.
  
  (let [;; Initialize each window start time with a noop.
        ;; It's important to do this first, otherwise we may
        ;; miss windows where nothing changes availability-wise.
        windows (zipmap (range from to w) (cycle [[]]))
        ;; Generate all ops to apply.
        ops (reduce concat [] (concat (map (partial avail->ops from to w) avails)
                                      (map (partial appt->ops from to w) appts)))
        ;; Map each op to its window start time (edge),
        ;; so we can loop through all window edges and 
        ;; iteratively determine resource availability.
        ;; Sort by starting window edge so we iterate
        ;; over all ops in order.
        edge->ops (sort-by first (reduce fold-ops windows ops))
        ;; Store a map from resource id to toggle,
        ;; AKA whether that resource is available during
        ;; this window: 1 for available, 0 for unavailable.
        ;; We start off with no information, and assume all resources
        ;; are unavailable (0).
        id->toggle (atom (zipmap (map second ops) (cycle [0])))]
    (reduce (fn [windows [t ops]]
              ;; Get the latest availability info.
              (swap! id->toggle apply-ops ops)
              (let [ids (->ids @id->toggle)]
                (if (seq ids)
                  ;; We have at least one availability. Add it to the list.
                  (conj windows {:start t
                                 :end (+ t w)
                                 :ids ids})
                  ;; No availabilities for this window; keep going.
                  windows)))
            []
            edge->ops)))

(comment

  ;; Imaginary provider ids:
  ;; * 1 - Shevek
  ;; * 2 - Takver
  ;; * 3 - Genly Ai
  (def avails [;; DAY 0: only Takver
               ;; 1-5 Takver
               {:start 100 :end 500 :id 2}

               ;; DAY 1: No availabilities.

               ;; DAY 2: Shevek & Takver available w/ some overlap
               ;; 2-4 Shevek
               {:start 2200 :end 2500 :id 1}
               ;; 3-6 Takver
               {:start 2300 :end 2600 :id 2}

               ;; DAY 3: Genly Ai available for two distinct timeframes
               ;; 3-5 Genly
               {:start 3300 :end 3500 :id 3}
               ;; 7-9
               {:start 3300 :end 3500 :id 3}])

  (->windows (take 1 avails) [] 0 300 50)

  ;;  
  )