(ns rtc.intake.core
  (:require
   [reagent.dom :as dom]
   [re-frame.core :as rf]))


(defn intake-form []
  [:div "TODO"])


(defn ^:dev/after-load mount! []
  (dom/render [intake-form] (.getElementById js/document "rtc-intake")))

(defn init! []
  (mount!))