(ns rtc.appointments.core)


(defn book-appointment-resolver [_context args _value]
  (println "BOOK" args))