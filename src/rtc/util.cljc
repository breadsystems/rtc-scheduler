(ns rtc.util)


(defn ->opt [x]
  (if (map? x)
    x
    {:value x :label x}))

(defn index-by [f xs]
  (into {} (map #(vector (f %) %) xs)))