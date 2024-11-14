(ns rtc.actions)

(defmulti act! (fn [action _chan] (:action/type action)))

(defmethod act! :open [{:action/keys [timestamp]} _]
  ;; TODO log info
  (println "connection opened at" timestamp))
