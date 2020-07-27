(ns rtc.admin.events)

(defmulti ->fc-event :event/type)
(defmethod ->fc-event :default [e] e)
(defmethod ->fc-event :availability [event]
  (assoc event
         :title "Available"
         :editable true
         :backgroundColor "#325685"))
(defmethod ->fc-event :appointment [event]
  (assoc event
         :editable false))