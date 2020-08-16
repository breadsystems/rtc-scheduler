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
(def ^:dynamic *js-dir* "public/js")

(defn- style-manifest []
  (some-> (io/resource (str *css-dir* "/manifest.edn"))
          slurp
          edn/read-string))

(defn- stylesheet-manifest-entry [asset-name]
  (some->> (style-manifest) (filter #(= asset-name (:name %))) first))

(defn style-href [asset-name]
  (let [entry (stylesheet-manifest-entry asset-name)]
    (str "/css/" (or (:file entry) (str (name asset-name) ".css")))))


(defn- js-manifest []
  (some-> (io/resource (str *js-dir* "/manifest.edn"))
          slurp
          edn/read-string))

(defn- js-manifest-entry [asset-name]
  (some->> (js-manifest) (filter #(= asset-name (:name %))) first))

(defn js-src [asset-name]
  (let [entry (js-manifest-entry asset-name)]
    (str "/js/" (or (:output-name entry) (str (name asset-name) ".js")))))

(comment
  (style-href :intake)
  (style-href :admin)

  (js-src :shared)
  (js-src :intake)
  (js-src :admin))