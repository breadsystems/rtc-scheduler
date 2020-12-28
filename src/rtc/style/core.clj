(ns rtc.style.core
  (:refer-clojure :exclude [> *])
  (:require
   [garden.color :refer [rgb as-hex]]
   [garden.def :refer [defcssfn]]
   [garden.selectors :refer [attr defselector > &]]))


(defselector *)

(defcssfn url)
(defcssfn calc)

(def pink (rgb 255 26 79))
(def light-pink (rgb 255 220 225))
(def muted-pink (rgb 185 90 115))
(def off-white (rgb 255 240 250))
(def purple "#b9a1fb")
(def indigo (rgb 127 90 240))
(def dark-purple (rgb 110 25 160))
(def muted-purple (rgb 55 5 80))
(def grey (rgb 100 100 100))
(def light-grey (rgb 245 245 245))
(def dark-grey (rgb 115 105 135))

(def purple-border (str "2px solid " purple))
(def border-radius "0.3em")

;; Button mixins
(def button-base {:padding "0.7em 1.3em"
                  :border-width "3px"
                  :border-style :solid
                  :border-color pink
                  :cursor :pointer
                  :font-weight 700
                  :text-transform :uppercase
                  :color :white
                  :background pink})

(def button-secondary (merge button-base {:background indigo
                                          :border-color indigo}))

(def button-disabled {:background-color muted-pink
                      :border-color muted-pink
                      :color off-white
                      :cursor :not-allowed})

(def button-disabled-secondary
  (merge button-disabled {:background-color muted-purple
                          :border-color muted-purple
                          :color off-white}))


(def base
  [[:* {:box-sizing :border-box}]
   [:body {:width "80em"
           :max-width "90%"
           :margin "1em auto"
           :padding-top "2em"
           :line-height 1.5

           :font-family "'Libre Franklin', sans-serif"
           :font-weight 400}]
   [:section {:margin-bottom "2em"}]
   [:dl {:display :grid
         :grid-template-columns "1fr 5fr"}]
   [:dt {:margin-bottom "0.3em"
         :font-weight 700}]
   [:.slack]])

(def typography
  [;; Main typographical elements
   [:header {:text-align :center}]
   [:h1 {:color pink
         :text-transform :uppercase}]
   [:h2 :h3 :h4 :h5 :h6
    {:color dark-purple}]
   [:a {:color dark-purple}]
   [:.prose {:margin "3em auto"
             :max-width "40em"}]

   ;; Text styles
   [:.call-to-action
    button-base
    {:font-size "1.2em"
     :text-decoration :none
     :background dark-purple
     :border-color dark-purple}
    [:&:focus {:background :white
               :color dark-purple}]
    [:&:hover {:background :white
               :color dark-purple}]]
   [:.help {:color grey
            :font-style :italic}]
   [:.highlight {:color pink}]
   [:.spacious {:margin-top "3em"
                :margin-bottom "3em"}]
   [:.instruct {:font-size "0.9em"
                :color dark-grey
                :font-style :italic}]

   ;; Alignments
   [:.center {:text-align :center}]
   [:.right {:text-align :right}]])

(def nav
  [[:nav
    [:ul {:display :flex
          :flex-wrap :wrap
          :margin "3em 0 3em"
          :padding 0
          :width "100%"
          :justify-content :space-around
          :list-style :none}
     [:li {:flex "1 1 10ch"
           :margin "1em"}]]]
   [:.nav-link {:color dark-purple
                :cursor :pointer
                :font-size "1.2em"
                :text-decoration :none
                :font-weight 700}]
   [:.current
    [:.nav-link {:display :inline
                 :text-decoration :underline
                 :text-decoration-skip-ink :auto
                 :text-decoration-thickness "2px"}]]])

(def i18n
  [[:.lang-selector {:position :absolute
                     :top 0
                     :left 0
                     :width "100%"
                     :z-index 2
                     :text-align :right
                     :background light-grey}
    [:* {:font-size "0.8em"}]
    [:label {:margin "1em"}]
    [:select {:margin "0.3em"
              :width "10rem"}]]])

(def purple-box {:padding "0.7em"
                 :border-radius border-radius
                 :border purple-border})
(def &purple-box-focus [:&:focus {:outline :none
                                  :border-color dark-purple}])

(def box-field {:width "25rem"
                :max-width "100%"
                :border purple-border})

(def forms
  [[:input [(& (attr :type=text))
            (& (attr :type=email))
            (& (attr :type=password))
            box-field
            purple-box
            &purple-box-focus]
    [:&+label {:cursor :pointer}]
    [:&:disabled {:cursor :not-allowed}
     [:&+label {:cursor :not-allowed}]]]
   [:select
    box-field
    purple-box
    {:background-color :lavender
     :background-image (url "/img/caret-down.svg")
     :background-position "right 10px bottom 12px"
     :background-repeat :no-repeat
     :-moz-appearance :none
     :-webkit-appearance :none}
    &purple-box-focus
    [:&:ms-expand {:display :none}]]
   [:.field {:margin "0.7em 0"}]
   [:.radio-option {:margin "0 2em 0 0"}]
   [:.required {:color pink}]
   [:.has-errors {:color dark-purple
                  :background-color light-pink
                  :border-color muted-pink}]
   [:.error-message {:color muted-pink
                     :font-weight 700
                     :margin "0.3em 0"}]
   [:button :.button button-base
    [:&.secondary button-secondary
     [:&:focus {:background-color :white
                :color (as-hex pink)}]
     [:&:disabled :&.disabled button-disabled-secondary]]
    [:&.next {:text-align :right}]
    [:&:focus {:background-color :white
               :color (as-hex pink)}]
    [:&:disabled :&.disabled button-disabled]]
   [:.text-button {:font-weight 700
                   :color indigo
                   :cursor :pointer}
    [:&:disabled :&.disabled {:cursor :not-allowed
                              :color dark-grey}]]
   [:legend {:color dark-purple
             :font-weight 700}]
   [:.fc-button {:opacity 1
                 :background dark-purple
                 :border :none
                 :font-weight 700
                 :text-transform :uppercase}
    [:&:disabled button-disabled-secondary]]])

(def states
  [[:.loading {:opacity 0.5
               :cursor :wait}]])
