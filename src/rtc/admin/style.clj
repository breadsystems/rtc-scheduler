(ns rtc.admin.style
  (:require
   [rtc.style.core :as core]
   [garden.def :refer [defstyles]]
   #_[garden.stylesheet :refer [at-media]]))

(def calendar
  [[:.rtc-appointment {:cursor :pointer}]])


(defstyles screen
  core/base
  core/nav
  core/typography
  core/forms
  core/states
  calendar)