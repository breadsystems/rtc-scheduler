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

(def state-mappings
  {"CA" #{"CA"}
   "MN" #{"MN"}
   "OH" #{"OH"}
   "WA" #{"WA"}
   "WI" #{"WI"}})