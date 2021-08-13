(ns rtc.util
   #?(:clj
    (:import
      [java.time ZonedDateTime ZoneId]
      [java.time.format DateTimeFormatter]
      [java.util Date])))


(defn ->zoned
  "Takes a java.util.Date object and converts to a java.time.ZonedDateTime
  with the timezone named by the given string."
  [date zone-str]
  #?(:clj
      (ZonedDateTime/ofInstant (.toInstant date) (ZoneId/of zone-str))))

(defn format-zoned [dt pattern]
  #?(:clj
     (let [fmt (DateTimeFormatter/ofPattern pattern)]
       (.format fmt dt))))

(comment

  (->zoned #inst "2021-07-10T01:00:00.000000000-00:00" "America/New_York")
  (->zoned #inst "2021-07-10T01:00:00.000000000-00:00" "America/Los_Angeles")

  (format-zoned
    (->zoned #inst "2021-07-10T01:00:00.000000000-00:00" "America/Los_Angeles")
    "h:mma z")

  ;;
  )

;; Date/time helpers
(def thirty-minutes (* 30 60 1000))
(def one-hour (* 60 60 1000))
(def six-hours (* 6 60 60 1000))
(def one-day (* 24 60 60 1000))
(def one-week (* 7 24 60 60 1000))

(defn midnight-this-morning []
  #?(:clj
     (doto (Date.)
       (.setHours 0)
       (.setMinutes 0)
       (.setSeconds 0))
     :cljs
     (throw (js/Error. "Not implemented!"))))


(defn ->opt [x]
  (if (map? x)
    x
    {:value x :label x}))

(defn index-by [f xs]
  (into {} (map #(vector (f %) %) xs)))
