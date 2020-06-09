;; Tooling namespace for building CSS from Garden stylesheets.
;; https://github.com/noprompt/garden
(ns rtc.style.build
  (:require
   [garden.core :as garden]
   [ns-tracker.core :refer [ns-tracker]]))


(defn- compile-css-on-change [build]
  (let [{:keys [source-paths styles compiler interval-ms]} build
        tracker (ns-tracker source-paths)
        ns-sym (symbol (namespace styles))]
    (loop [namespaces (tracker)]
      (try
        (when (seq namespaces)
          (require ns-sym :reload)
          (let [styles-list (deref (resolve (:styles build)))
                [conf rules] (if (map? (first styles-list))
                               [(first styles-list) (next styles-list)]
                               [{} styles-list])]
            ;; TODO why doesn't this work the first time?
            (apply garden/css (merge conf compiler) rules)))
        (catch Exception e
          (println "Error compiling CSS:" (.getMessage e) e)))
      (Thread/sleep (or interval-ms 100))
      (flush)
      (recur (tracker)))))

(defn watch!
  "Watch namespaces for changes, compiling CSS when a stylesheet or
   any of its dependencies is updated."
  [build]
  (.start (Thread. (fn []
                     (compile-css-on-change build)))))