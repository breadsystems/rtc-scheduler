(ns rtc.i18n.data
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))


(defn reduce-langs [ms]
  (reduce (fn [data m]
            (assoc data (:lang m) m)) {} ms))

(def ^:dynamic i18n-path "./resources/i18n")

(defn- load-from-files [path]
  (->> path
       (io/file)
       (.listFiles)
       (map (comp edn/read-string slurp))
       (reduce-langs)))

(comment
  (reduce-langs [{:lang :en :yes "Yes"} {:lang :es :yes "Sí"}])
  ;; => {:en {:lang :en, :yes "Yes"}, :es {:lang :es, :yes "Sí"}}

  (slurp "./resources/i18n/en.edn")
  (load-from-files i18n-path))


(defmacro i18n-data []
  (load-from-files i18n-path))