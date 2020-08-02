(ns rtc.admin.style
  (:require
   [rtc.style.core :as core]
   [rtc.style.colors :as colors]
   [garden.def :refer [defstyles]]
   [garden.color :refer [rgba]]))

(def calendar
  [[:.care-schedule {:display :grid
                     :grid-template-columns "1fr 4fr"
                     :grid-gap "1em"}]
   [:.rtc-appointment {:cursor :pointer}]
   [:.fc-event.rtc-availability {:cursor :default}
    [:&.rtc-draggable {:cursor :grab}]]
   [:.rtc-delete {:position :absolute
                  :display :block
                  :right 0
                  :top 0
                  ;; :padding "0.5em"
                  :width "1em"
                  :height "1em"
                  :z-index 2

                  :cursor :pointer
                  :color :white
                  :font-weight 700
                  :font-size "1.5rem"
                  :font-style :normal}]
   [:.filter-group {:margin-bottom "3em"}]
   [:.filter-field {:margin "1em 0"}]
   [:.filter-label {:margin-left "0.3em"
                    :cursor :pointer}
    [:&.filter-label--provider {:border-bottom-width "5px"
                                :border-bottom-style "solid"}]]
   [:.access-needs-legend [:div {:margin "0.3em"}]]
   [:.access-needs-indicator {:padding "0.3em"
                              :display :inline-block
                              :border-radius "3px"
                              :border-width "1px"
                              :border-style :solid
                              :color :white}
    [:&.--met {:background-color colors/appointment-fulfilled-bg
                         :border-color colors/appointment-fulfilled-border}]
    [:&.--unmet {:background-color colors/appointment-unfulfilled-bg
                           :border-color colors/appointment-unfulfilled-border}]]])

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

(def appointment
  [[:.appointment-details {:display :grid
                           :grid-template-columns "1fr 1fr"}]
   [:.appointment-name {:color core/pink}]])


(defstyles screen
  core/base
  core/nav
  core/typography
  core/forms
  core/states
  calendar
  appointment
  modal)