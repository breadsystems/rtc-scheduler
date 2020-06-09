(ns rtc.intake.style
  (:require
   [rtc.style.core :as core]
   [garden.def :refer [defstyles]]))


(def questions
  [[:.question {:margin "2em 0"}]
   [:.field-label {:font-weight 700}]
   [:.intake-footer {:display :flex
                     :max-width "65em"
                     :margin "5em auto"
                     :justify-content :space-between}]])


(defstyles screen
  core/base
  core/typography
  core/forms
  questions)