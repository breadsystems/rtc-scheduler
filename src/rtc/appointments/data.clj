(ns rtc.appointments.data
  (:require
   [rtc.db :as d]
   [clj-time.coerce :as c]))


(defn create! [])


(comment
  (d/bind!)

  (d/create-need! {:name "Interpretation"
                   :description "Translation service for a non-English speaker"})
  (d/get-needs)
  (d/get-need {:id 1})

  (d/create-appointment! {:start (c/to-sql-time #inst "2020-08-09T10:30:00.000-08:00")
                          :end (c/to-sql-time #inst "2020-08-09T10:50:00.000-08:00")
                          :reason "I have questions about my HRT"
                          :careseeker-id 1
                          :provider-id 2})
  (d/create-appointment! {:start (c/to-sql-time #inst "2020-08-10T09:00:00.000-08:00")
                          :end (c/to-sql-time #inst "2020-08-10T09:20:00.000-08:00")
                          :reason "I have questions about my medication"
                          :careseeker-id 2
                          :provider-id 1})

  (d/get-appointment {:id 1})
  (d/get-appointment {:id 2})

  (d/get-provider {:user-id 2})

  (d/create-availability! {:start (c/to-sql-time #inst "2020-08-11T11:30:00.000-08:00")
                           :end   (c/to-sql-time #inst "2020-08-11T17:00:00.000-08:00")
                           :provider-id 2})
  (d/create-availability! {:start (c/to-sql-time #inst "2020-08-12T14:30:00.000-08:00")
                           :end   (c/to-sql-time #inst "2020-08-12T18:00:00.000-08:00")
                           :provider-id 2})

  (d/get-availabilities {:join (d/join-provider-availability)
                         :where
                         (d/where-available
                          {:cond [(d/available-between
                                   {:conj ""
                                    :start (c/to-sql-time #inst "2020-08-11T09:00:00.000-08:00")
                                    :end   (c/to-sql-time #inst "2020-08-13T17:00:00.000-08:00")})
                                  (d/available-in-state
                                   {:conj "AND"
                                    :state "OR"})]})})

  (d/create-appointment-need! {:need-id 1 :appointment-id 1 :info "Mandarin"})
  (d/get-appointment-need {:appointment-id 1})
  (d/delete-appointment-need! {:need-id 1 :appointment-id 1}))