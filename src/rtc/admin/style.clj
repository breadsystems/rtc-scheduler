(ns rtc.admin.style
  (:require
   [rtc.style.core :as core]
   [garden.def :refer [defstyles]]
   #_[garden.stylesheet :refer [at-media]]))

(def calendar
  [[:.rtc-appointment {:cursor :pointer}]
   [:.rtc-delete {:position :absolute
                  :display :block
                  :right 0
                  :top 0
                  :width "1em"
                  :height "1em"
                  :z-index 2

                  :color :white
                  :font-weight 700
                  :font-size "16px"
                  :font-style :normal}]])


(defstyles screen
  core/base
  core/nav
  core/typography
  core/forms
  core/states
  calendar)