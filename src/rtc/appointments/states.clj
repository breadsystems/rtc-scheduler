(ns rtc.appointments.states)


;; SUMMARY OF STATE LICENSING
;;
;; By Jay as of 2020-08-27:
;;
;; Patients from these states can only have a provider licensed in that state provide telehealth services:
;; * California
;; * Minnesota
;; * Ohio
;; * Washington
;; * Wisconsin
;;
;; States with automatic waivers of license requirements to volunteer via telehealth due to COVID-19 (anyone with a
;; license can practice in these states):
;; * Colorado
;; * Connecticut
;; * Delaware
;; * Florida
;; * Hawaii
;; * Idaho
;; * Iowa
;; * Indiana
;; * Michigan
;; * Missouri
;; * Nebraska
;; * New Hampshire
;; * New Mexico
;; * New York
;; * North Carolina
;; * Pennsylvania
;; * Vermont
;;
;; According to Pat, Florida has now restricted their license to in-house only
;; TODO confirm and address above?
;;
;; Virginia & Wyoming automatic only where an existing physician-patient relationship was established prior and for
;; ongoing medical care, no new diagnoses or conditions
;; TODO not sure what this means?
;;
;; States that require an Emergency License Applications to provide telehealth due to COVID-19:
;; * Alabama
;; * Alaska
;; * Arizona
;; * Georgia
;; * Illinois
;; * Kansas
;; * Kentucky
;; * Louisiana
;; * Maine
;; * Massachusetts
;; * Mississippi
;; * Montana
;; * New Jersey
;; * North Dakota
;; * Oklahoma
;; * Oregon
;; * Rhode Island
;; * South Carolina
;; * South Dakota
;; * Tennessee
;; * Texas
;; * Utah
;; * West Virginia
;; * Wyoming.
;;
;; Link to the original document Riley made:
;;
;; https://docs.google.com/document/d/1k0MXd1gwyaleigCPqYLN6G028SfUV7lCXitBc8Ol8ro/edit#
;;
;; Cheers,
;; Jay

(def US-STATES
  ^{:doc
    "All US states, plus DC"}
  #{"AL" "AK" "AZ" "AR" "CA" "CO" "CT" "DE" "FL" "GA"
    "HI" "ID" "IL" "IN" "IA" "KS" "KY" "LA" "ME" "MD"
    "MA" "MI" "MN" "MS" "MO" "MT" "NE" "NV" "NH" "NJ"
    "NM" "NY" "NC" "ND" "OH" "OK" "OR" "PA" "RI" "SC"
    "SD" "TN" "TX" "UT" "VT" "VA" "WA" "WV" "WI" "WY"
    "DC"})

(def
  ^{:doc
    "Maps a Careseeker's state to the states in which a Provider can practice
    in order to provide care for the Careseeker. For example:
    - CO -> US-STATES means that providers in any US state can provide care
      for Careseekers in Colorado.
    - CA -> #{CA} means only Providers in California can provide telehealth for
      Careseekers in California."}
  state-mappings
  {;; Patients from these states can only have a provider licensed in that
   ;; state provide telehealth services:
   ;; * California
   ;; * Minnesota
   ;; * Ohio
   ;; * Washington
   ;; * Wisconsin
   "CA" #{"CA"}
   "MN" #{"MN"}
   "OH" #{"OH"}
   "WA" #{"WA"}
   "WI" #{"WI"}

   ;; States with automatic waivers of license requirements to volunteer via
   ;; telehealth due to COVID-19 (anyone with a license can practice in these
   ;; states):
   "CO" US-STATES
   "CT" US-STATES
   "DC" US-STATES
   "DE" US-STATES
   "FL" US-STATES
   "HI" US-STATES
   "ID" US-STATES
   "IA" US-STATES
   "MA" US-STATES
   "MI" US-STATES
   "MO" US-STATES
   "NE" US-STATES
   "NH" US-STATES
   "NM" US-STATES
   "NY" US-STATES
   "NC" US-STATES
   "PA" US-STATES
   "VT" US-STATES
   })
