;; Tooling namespace for building CSS from Garden stylesheets.
;; https://github.com/noprompt/garden
(ns rtc.style.build
  (:require
   [garden.core :as garden]
   [ns-tracker.core :refer [ns-tracker]]))


(defn watch!
  "Watch namespaces for changes, compiling CSS when a stylesheet or
   any of its dependencies is updated."
  [{:keys [source-paths styles compiler interval-ms]}]
  (let [tracker (ns-tracker source-paths)
        ns-sym (symbol (namespace styles))]
    ;; TODO use go-loop
    (loop [namespaces (tracker)]
      (when (seq namespaces)
        (require ns-sym :reload)
        (let [styles-list (deref (resolve styles))
              [conf rules] (if (map? (first styles-list))
                             [(first styles-list) (next styles-list)]
                             [{} styles-list])]
          (apply garden/css (merge conf compiler) rules)))
      (Thread/sleep (or interval-ms 100))
      (recur (tracker)))))