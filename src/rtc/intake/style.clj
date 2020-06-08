(ns rtc.intake.style
  (:require
   [rtc.style.core :as core]
   [garden.def :refer [defstyles]]))


(defstyles screen
  core/base
  [:h1 {:color :black}]
  [:h2 {:color :red}])