(ns rtc.admin.style
  (:require
   [rtc.style.core :as core]
   [garden.def :refer [defstyles]]
   [garden.color :refer [rgba]]))

(def calendar
  [[:.rtc-appointment {:cursor :pointer}]
   [:.rtc-delete {:position :absolute
                  :display :block
                  :right 0
                  :top 0
                  ;; :padding "0.5em"
                  :width "1em"
                  :height "1em"
                  :z-index 2

                  :color :white
                  :font-weight 700
                  :font-size "1.5rem"
                  :font-style :normal}]])

(def modal
  [[:.modal {:position :absolute
             :width "80vw"
             :height "80vh"
             :top "10vh"
             :left "10vh"
             :padding "1em"
             :background :white}
    [:h2 {:margin-top 0}]]
   [:.modal-bg {:position :fixed
                :width "100vw"
                :height "100vh"
                :top 0
                :left 0
                :background (rgba 0 0 0 0.8)
                :z-index 3}]
   [:.modal__close {:position :absolute
                    :top 0
                    :right "0.5rem"
                    :color :darkslategrey
                    :font-size "2em"
                    :font-weight 700
                    :cursor :pointer}]])


(defstyles screen
  core/base
  core/nav
  core/typography
  core/forms
  core/states
  calendar
  modal)