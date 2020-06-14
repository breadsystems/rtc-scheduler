(ns rtc.style.core
  (:require
   [garden.color :refer [rgb]]
   [garden.def :refer [defcssfn]]
   [garden.selectors :refer [attr &]]))


(defcssfn url)
(defcssfn calc)

(def pink (rgb 255 26 79))
(def light-pink (rgb 255 220 225))
(def muted-pink (rgb 185 90 115))
(def off-white (rgb 255 230 250))
(def purple "#b9a1fb")
(def dark-purple (rgb 110 25 160))
(def muted-purple (rgb 55 5 80))
(def grey (rgb 100 100 100))
(def dark-grey (rgb 115 105 135))

(def purple-border (str "2px solid " purple))


(def base
  [])

(def typography
  [[:h1 {:color pink
         :text-transform :uppercase}
    [:h2 :h3 :h4 :h5 :h6
     {:color dark-purple}]]
   [:a {:color dark-purple}]
   [:.help {:color grey
            :font-style :italic}]])


(def button-base {:padding "0.7em 1.3em"
                  :border :none
                  :cursor :pointer
                  :font-weight 700
                  :text-transform :uppercase
                  :color :white
                  :background pink})

(def button-secondary (merge button-base {:background purple}))

(def button-disabled {:background-color muted-pink
                      :color off-white
                      :cursor :not-allowed})

(def button-disabled-secondary
  (merge button-disabled {:background-color muted-purple
                          :color off-white}))

(def forms
  [[:.field {:margin "0.7em 0"}]
   [:.radio-option {:margin "0 2em 0 0"}]
   [:.required {:color pink}]
   [:.has-errors {:color dark-purple
                  :background-color light-pink
                  :border-color muted-pink}]
   [:.error-message {:color muted-pink
                     :font-weight 700
                     :margin "0.3em 0"}]
   [:input [(& (attr :type=text))
            (& (attr :type=email))
            (& (attr :type=password))
            {:min-width "25rem"
             :padding "0.7em"
             :border-radius "0.3em"
             :border purple-border}]]
   [:button :.button button-base
    [:&.secondary button-secondary
     [:&:disabled :&.disabled button-disabled-secondary]]
    [:&.next {:text-align :right}]
    [:&:disabled :&.disabled button-disabled]]
   [:select {:width "25rem"
             :max-width "100%"
             :border purple-border
             :border-radius "0.3em"
             :background-color :lavender
             :background-image (url "/img/caret-down.svg")
             :background-position "right 10px bottom 12px"
             :background-repeat :no-repeat
             :padding "0.7em"
             :-moz-appearance :none
             :-webkit-appearance :none}
    [:&:ms-expand {:display :none}]]
   [:.fc-button {:opacity 1
                 :background dark-purple
                 :border :none
                 :font-weight 700
                 :text-transform :uppercase}
    [:&:disabled button-disabled-secondary]]])
