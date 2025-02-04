(ns rtc.schema
  (:require
    [systems.bread.alpha.core :as bread]))

(def
  ^{:doc "Base schema for RTC web app."}
  base
  (with-meta
    [{:db/id "migration.base"
      :migration/key :rtc.migration/base
      :migration/description "Base schema for appointments, providers, and schedulers."}

     ;; Appointments

     ;; NOTE:
     ;; - appointments will use :thing/uuid
     ;; - no usage of :thing/order, date-based instead
     ;; - no hierarchy
     ;; - available days/times are represented as :thing/fields
     {:db/ident :appt/name
      :attr/label "Name"
      :db/doc "Name of the person seeking care."
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}
     {:db/ident :appt/pronouns
      :attr/label "Pronouns"
      :db/doc "Pronouns of the person requesting an appointment, if given."
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}
     {:db/ident :appt/email
      :attr/label "Email"
      :db/doc "Email of the person requesting an appointment, if given."
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}
     {:db/ident :appt/phone
      :attr/label "Phone"
      :db/doc "Phone number of the person requesting an appointment, if given."
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}
     {:db/ident :appt/state
      :attr/label "State"
      :db/doc "State where the person requesting an appointment is located."
      :db/valueType :db.type/keyword
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}
     {:db/ident :appt/status
      :attr/label "Status"
      :db/doc "Current status of the appointment."
      :db/valueType :db.type/keyword
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}
     {:db/ident :appt/text-ok?
      :attr/label "Text OK"
      :db/doc "Whether the person seeking care is OK with receiving text messages."
      :db/valueType :db.type/boolean
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}
     {:db/ident :appt/preferred-comm
      :attr/label "Preferred comm. channel"
      :db/doc "The preferred communication channel (text/email) of the person seeking care."
      :db/valueType :db.type/keyword
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}
     {:db/ident :appt/reason
      :attr/label "Reason"
      :db/doc "Reason for requesting the appointment."
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}
     {:db/ident :appt/access-needs
      :attr/label "Access needs"
      :db/doc "Access needs of the person seeking care."
      :db/valueType :db.type/ref
      :db/cardinality :db.cardinality/many
      :attr/migration "migration.base"}
     {:db/ident :appt/misc
      :attr/label "Miscellaneous"
      :db/doc "Anything we forgot to ask?"
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}

     ;; Access needs

     ;; NOTE: if we need to add metadata to an Access Need we can use fields.
     {:db/ident :need/type
      :attr/label "Access need type"
      :db/doc "Type of access need."
      :db/valueType :db.type/keyword
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}
     {:db/ident :need/met?
      :attr/label "Access need met"
      :db/doc "Whether this access need has been met."
      :db/valueType :db.type/boolean
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}

     ;; Notes

     ;; created-at comes from :thing/*
     {:db/ident :appt/notes
      :attr/label "Appointment notes"
      :db/doc "Notes for a given Appointment."
      :db/valueType :db.type/ref
      :db/cardinality :db.cardinality/many
      :attr/migration "migration.base"}
     {:db/ident :note/created-by
      :attr/label "Note author"
      :db/doc "The user who wrote this note."
      :db/valueType :db.type/ref
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}
     {:db/ident :note/content
      :attr/label "Note content"
      :db/doc "The content of the note."
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :attr/migration "migration.base"}

     ;; TODO Providers

     ;;
     ]

    {:type :bread/migration
     :migration/dependencies #{:bread.migration/migrations
                               :bread.migration/things
                               :bread.migration/users}}))

(defmethod bread/action ::migrations [_ _ [all-migrations]]
  (concat all-migrations
          [base
          ;; NOTE: add new migrations here...

          ;;
          ]))
