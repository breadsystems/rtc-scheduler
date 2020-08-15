(ns rtc.intake.style
  (:require
   [rtc.style.core :as core]
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]))


(def nav
  [[:.nav-link
    [:&.disabled {:color core/dark-grey
                  :cursor :not-allowed}]]])

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

(def confirmation
  [[:.detail {:display :block
              :margin "0.7em 0"}
    (at-media {:min-width "740px"}
              [:& {:display :flex}
               [:div {:flex "0 0 35%"
                      :padding-right "2em"}]])]
   [:.confirm-container {:margin "3em auto"
                         :text-align :center}]])

(def i18n
  [[:.lang-selector {:position :fixed
                     :right "1em"
                     :bottom "1em"
                     :width "10rem"
                     :padding "1em"
                     :z-index 2
                     :background :white
                     :border core/purple-border
                     :border-radius core/border-radius}
    [:div {:margin-bottom "1em"}]]])


(defstyles screen
  core/base
  core/nav
  core/typography
  core/forms
  core/states
  nav
  questions
  calendar
  confirmation
  i18n)