(ns rtc.appointments.windows
  (:require
   [clojure.spec.alpha :as spec]
   [java-time :as t])
  (:import [java.time ZonedDateTime OffsetDateTime]
           [java.text SimpleDateFormat]))


(extend-protocol Inst
  OffsetDateTime
  (inst-ms* [inst] (t/to-millis-from-epoch inst)))

(spec/def ::date (spec/inst-in #inst "2000" #inst "2100"))

;; A window should be between 10m and 2h
(spec/def ::window-minutes (spec/and int? #(> % 10) #(> 120 %)))

(spec/def ::provider-id #{1 2 3 4 5 6})


(defn drange->window-count [from to window-in-minutes]
  (/ (- (inst-ms to) (inst-ms from)) (* window-in-minutes 60 1000)))

(defn drange->windows [from to window-in-minutes]
  (let [window-count (drange->window-count from to window-in-minutes)]
    (map (fn [factor]
           [(t/plus from (t/duration (* factor window-in-minutes) :minutes))
            (t/plus from (t/duration (* (inc factor) window-in-minutes) :minutes))])
         (range 0 (inc window-count)))))


(defn availabilities->windows [avails appts from to w]

  ;; For each availability, compute the Timeframe that overlaps
  ;; with [from to].

  ;; For each Timeframe [A'n B'n], bread into chunks of length w
  ;; [[A'0 A'0+w] [A'0+w A'0+2w] ,,, [A'n-w A'n]]
  ;; Record in a map where the keys are vectors [A'n A'n+w]
  ;; and the values are maps with Provider data.

  ;; Merge all Timeframe maps together, conjoining (unique) Provider
  ;; values into sets. We now have a mapping of window start/end times
  ;; to *potentially* available providers (we just need to take
  ;; existing appointments into account).

  ;; For each Appoinment, round start (down) and end (up) to nearest
  ;; window edge, i.e. (+ from (* n w)), where n is an int, to get
  ;; [start'r end'r]. Remove the Provider from the set indexed by
  ;; [start'r end'r].

  ;; The Windows remaining with at least one Provider are our
  ;; Available Windows.

  ;;  
  )


(comment
  ;; Just playing with Inst
  (inst-ms #inst "2020-03-04T00:00:00-07:00") ;; => 1583305200000

  ;; July 1st - July 15th
  ;; => 14 * 48 = 672
  (let [from (OffsetDateTime/parse "2020-07-01T00:00:00-07:00")
        to   (OffsetDateTime/parse "2020-07-15T00:00:00-07:00")]
    (drange->window-count from to 30))

  ;; 9am - 6pm
  ;; 9h * 2w/h = 18
  (let [from (OffsetDateTime/parse "2020-07-01T09:00:00-07:00")
        to   (OffsetDateTime/parse "2020-07-01T18:00:00-07:00")
        windows (drange->windows from to 30)
        window->providers (into {} (map (fn [window]
                                          [window #{1 2 3}])
                                        windows))]
    (get window->providers [(OffsetDateTime/parse "2020-07-01T10:00:00-07:00")
                            (OffsetDateTime/parse "2020-07-01T10:30:00-07:00")]))
  ;; => #{1 3 2}

  {0 #{2}
   1 #{}
   2 #{1}
   3 #{3}
   4 #{1 2 3}
   5 #{1 3}
   6 #{1 2}
   7 #{}
   8 #{1 2 3}
   9 #{1 2 3}
   10 #{1 3}
   11 #{1 3}}


  (as-> "2020-07-01T11:00:00-07:00" $
    (OffsetDateTime/parse $)
    (t/plus $ (t/duration 20 :minutes))
    (str $))
  ;; => "2020-07-01T11:20-07:00"

  (t/max (OffsetDateTime/parse "2020-03-04T00:00:00-07:00")
         (OffsetDateTime/parse "2020-03-04T00:00:00-08:00"))
  ;; => #object[java.time.OffsetDateTime 0x4abb3ece "2020-03-04T00:00-08:00"]
  (str (t/max (OffsetDateTime/parse "2020-03-04T00:00:00-07:00")
              (OffsetDateTime/parse "2020-03-04T00:00:00-08:00")))
  ;; => "2020-03-04T00:00-08:00"

  ;; Midnight Eastern is before midnight Pacific.
  (t/before? (OffsetDateTime/parse "2020-03-04T00:00:00-03:00")
             (OffsetDateTime/parse "2020-03-04T00:00:00-07:00")) ;; => true
  ;; 8pm Eastern is before midnight Pacific.
  (t/before? (OffsetDateTime/parse "2020-03-04T00:00:00-07:00")
             (OffsetDateTime/parse "2020-03-04T08:00:00-03:00")) ;; => true

  ;;
  )