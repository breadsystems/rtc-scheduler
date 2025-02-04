(ns rtc.env
  (:require
    [aero.core :as aero]
    [buddy.hashers :as hashers]))

(defmethod aero/reader 'buddy/derive [_ _ [pw algo]]
  (hashers/derive pw {:alg algo}))

