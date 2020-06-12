(ns rtc.intake.style
  (:require
   [rtc.style.core :as core]
   [garden.def :refer [defstyles]]))


(def nav
  [[:ul.progress {:display :flex
                  :margin "3em 0 3em"
                  :padding 0
                  :width "100%"
                  :justify-content :space-around
                  :list-style :none}]
   [:.step-link {:text-decoration :none}]])

(def questions
  [[:.question {:margin "2em 0"}]
   [:.field-label {:font-weight 700
                   :color core/dark-purple}]
   [:.intake-footer {:display :flex
                     :max-width "65em"
                     :margin "3em auto"
                     :justify-content :space-between}]])


(defstyles screen
  core/base
  core/typography
  core/forms
  nav
  questions)