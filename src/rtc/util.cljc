(ns rtc.util
  (:import
   #?(:clj
      [java.util Date])))


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