(ns rtc.appointments.internal.timeframes)


(defn inst-min [a b]
  (if (< (inst-ms a) (inst-ms b)) a b))

(defn inst-max [a b]
  (if (> (inst-ms a) (inst-ms b)) a b))

(defn before? [a b]
  (< (inst-ms a) (inst-ms b)))

(defn after? [a b]
  (> (inst-ms a) (inst-ms b)))

(defn plus [date ms]
  (java.util.Date. (+ (inst-ms date) ms)))

(defn minus [date ms]
  (java.util.Date. (- (inst-ms date) ms)))

(defn avail->timeframe
  "Takes an availability map and a date range and returns the overlapping
   timeframe."
  [{:keys [start end] :as avail} from to]
  ;; First determine that there's any overlap at all,
  ;; otherwise return nil.
  (when (and (before? from end)
             (before? start to))
    (assoc avail
           :start (inst-max start from)
           :end   (inst-min end to))))

(defn timeframe->window-map
  "Takes a timeframe and a window length (in minutes) and returns a map
   where keys are time windows"
  [{:keys [start end provider-id]} w]
  (let [window-millis (* w 60 1000)
        ;; Determine start dates for each window such that every window is
        ;; exactly w minutes, and doesn't run over end.
        window-starts (filter
                       #(>= (inst-ms end) (+ % window-millis))
                       (range (inst-ms start) (inst-ms end) window-millis))
        ;; Each key in the returned map is a window, a [start end] vector
        start-ms->window (fn [start-ms]
                           [(java.util.Date. start-ms)
                            (java.util.Date. (+ start-ms window-millis))])]
    ;; Reduce over our window starts to return a map keyed by windows.
    (reduce (fn [windows start-ms]
              (assoc windows (start-ms->window start-ms) provider-id))
            {}
            window-starts)))

(defn earliest-window-edge
  "Takes a Date start and an \"edge\" Date edge and finds the start time
   of the earliest window of length w that overlaps with start."
  [start edge w]
  (let [step (if (before? start edge) minus plus)]
    (loop [edge edge x 1]
      (when (> x 100) (throw (ex-info "too much recurz" {})))
      (if (and (before? edge start) (before? start (plus edge w)))
        edge
        (recur (step edge w) (inc x))))))

(defn appt->windows
  "Takes an appointment, a start date, and a window length (in minutes)
   and returns a vector of windows with which the appointment overlaps."
  [{:keys [start end]} from w]
  (let [window-millis (* w 60 1000)
        ;; Determine start dates for every window that overlaps with [start end].
        ;; Round start down to the nearest window edge.
        range-start (earliest-window-edge start from w)
        ;; Round end up to the nearest window edge.
        window-starts (range (inst-ms range-start)
                             (inst-ms end)
                             window-millis)
        start-ms->window (fn [start-ms]
                           [(java.util.Date. start-ms)
                            (java.util.Date. (+ start-ms window-millis))])]
    (reduce (fn [windows start-ms]
              (conj windows (start-ms->window start-ms)))
            []
            window-starts)))


(comment
  (= #inst "2020-01-01T07:00:00-00:00"
     #inst "2020-01-01T06:00:00-01:00"
     #inst "2020-01-01T00:00:00-07:00")
  
  (loop [s 100 from 155 w 50]
    (let [x (+ s w)]
      (if (>= x from)
        s
        (recur x from w))))

  (java.util.Date. (inst-ms #inst "2020-01-01")))