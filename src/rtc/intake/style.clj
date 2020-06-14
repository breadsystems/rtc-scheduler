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
   [:.step-link {:color core/dark-purple
                 :cursor :pointer
                 :font-size "1.2em"
                 :text-decoration :none
                 :font-weight 700}
    [:&.disabled {:color core/dark-grey
                  :cursor :not-allowed}]]
   [:.current
    [:.step-link {:display :inline
                  :text-shadow (str "-3px -3px white,"
                                    "-3px 3px white,"
                                    "3px -3px white,"
                                    "3px 3px white")
                  :background-size "1px 1em"
                  :box-shadow "inset 0 3px white, inset 0 -2px currentColor"}]]])

(def questions
  [[:.question {:margin "2em 0"}]
   [:.field-label {:font-weight 700
                   :color core/dark-purple}]
   [:.intake-footer {:display :flex
                     :max-width "65em"
                     :margin "3em auto 5em"
                     :justify-content :space-between}]])

(def calendar
  [[:.fc-list-item {:cursor :pointer}]])

(def i18n
  [[:.lang-selector {:position :fixed
                     :right "1em"
                     :bottom "1em"
                     :width "10rem"
                     :z-index 2}]])


(defstyles screen
  core/base
  core/typography
  core/forms
  nav
  questions
  calendar
  i18n)