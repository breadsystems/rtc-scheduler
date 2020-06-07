(ns rtc.util)


(defn ->opt [x]
  (if (map? x)
    x
    {:value x :label x}))