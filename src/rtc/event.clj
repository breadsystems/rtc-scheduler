;; Top-level abstraction for a system-wide pub/sub event stream.
(ns rtc.event
  (:require
    [clojure.core.async :refer [<! >! chan go go-loop mult tap untap]]))

(def ^:private events> (chan))
(def ^:private <events (mult events>))

(defn publish!
  "Publishes e, broadcasting to all subscribers (attached via subscribe!)"
  [e]
  (go (>! events> e)))

(defn- matcher [pattern]
  (let [pattern (if (map? pattern) pattern {:event/type pattern})
        pattern-keys (keys pattern)]
    (fn [e]
      (= pattern (select-keys e pattern-keys)))))

(comment
  ((matcher {}) {:a :b})
  ((matcher :x) {:event/type :x}))

(defn subscribe!
  "Subscribes (taps) to mult of the <events channel, attaching f as a handler
  for events that match the given pattern. If pattern is a map, each key from
  pattern must exist in e in order to match with e (other keys in e are
  ignored). If pattern is a keyword, matches on {:event/type pattern}.
  Returns an unsubscribe callback that closes around the mult (calls untap)."
  [pattern f]
  (let [listener (chan)
        matches? (matcher pattern)]
    (tap <events listener)
    (go-loop []
             (let [e (<! listener)]
               (when (matches? e)
                 (f e))
               (recur)))
    (fn []
      (untap <events listener))))

(comment
  (subscribe! :booked-appointment (fn [{appt :event/appointment}]
                                    (prn 'APPT! appt)))
  (publish! {:event/type :booked-appointment
             :event/appointment {:hi :there}}))
