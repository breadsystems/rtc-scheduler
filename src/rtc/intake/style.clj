(ns rtc.intake.style
  (:require
   [rtc.style.core :as core]
   [garden.def :refer [defstyles]]
   [garden.color :refer [as-hex]]
   [garden.stylesheet :refer [at-media]]))


(def nav
  [[:h1
    [:a {:color core/pink
         :text-decoration :none}
     [:&:hover {:text-decoration :underline}]]]
   [:.nav-link
    [:&.disabled {:color core/dark-grey
                  :cursor :not-allowed}]]])

(def questions
  [[:.question {:margin "2em 0"}]
   [:.field-label {:font-weight 700
                   :color (as-hex core/dark-purple)}]
   [:.intake-footer {:display :flex
                     :max-width "65em"
                     :margin "3em auto 5em"
                     :justify-content :space-between}]])

(def calendar
  [[:.fc-timegrid-event {:cursor :pointer}]
   [:.fc-list-item {:cursor :pointer}]])

(def confirmation
  [[:.detail {:display :block
              :margin "0.7em 0"}
    (at-media {:min-width "740px"}
              [:& {:display :flex}
               [:div {:flex "0 0 35%"
                      :padding-right "2em"}]])]
   [:.confirm-container {:margin "3em auto"
                         :text-align :center}]])


(defstyles screen
  core/base
  core/nav
  core/typography
  core/forms
  core/states
  core/i18n
  nav
  questions
  calendar
  confirmation)