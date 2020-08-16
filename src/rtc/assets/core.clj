(ns rtc.assets.core
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))


(defn wrap-asset-headers
  "Wrap static resource handlers to cache them for one year."
  [handler]
  (fn [req]
    (let [res (handler req)]
      (assoc-in res [:headers "Cache-Control"] "max-age=31536000"))))

(def ^:dynamic *css-dir* "public/css")

(defn- style-manifest []
  (some-> (io/resource (str *css-dir* "/manifest.edn"))
          slurp
          edn/read-string))

(defn- stylesheet-manifest-entry [asset-name]
  (some->> (style-manifest) (filter #(= asset-name (:name %))) first))

(defn style-href [asset-name]
  (let [entry (stylesheet-manifest-entry asset-name)]
    (str "/css/" (or (:file entry) (str (name asset-name) ".css")))))

(comment
  (style-href :intake)
  (style-href :admin))