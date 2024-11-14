(ns rtc.intake
  (:require
    [rtc.ui :as ui]))

(defn show [_]
  (ui/Page
    :content
    [:p "it's a page"]))
