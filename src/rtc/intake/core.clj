(ns rtc.intake.core
  (:require
   [rtc.i18n.core :as i18n :refer [t]]
   [rtc.layout :as layout]))


(defn get-care-handler
  "Intake form handlers. Renders a mini Single Page Application (SPA)
   for the person seeking care to fill in their details and book an
   appointment."
  [_req]
  (layout/intake-page {:title (t "Get Care" :en)}))

(defn new-careseeker-resolver
  "GraphQL resolver for intake form submission. Creates a new
   careseeker/appointment in the database and returns an
   appointment confirmation."
  [req]
  (layout/page {:content []}))