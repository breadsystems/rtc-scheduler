;; Tooling namespace for building CSS from Garden stylesheets.
;; https://github.com/noprompt/garden
(ns rtc.style.build
  (:require
   [garden.core :as garden]
   [ns-tracker.core :refer [ns-tracker]])
  (:import
   [java.security MessageDigest]))

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

(defn- md5 [^String s]
  (let [algorithm (MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))

(defn compile-garden-assets [assets]
  (mapv (fn [asset]
          (let [css (apply garden/css (:opts asset {}) (:styles asset))
                hash (md5 css)]
            {:name (:name asset)
             :hash hash
             :file (str (name (:name asset)) "." hash ".css")
             :contents css}))
        assets))

(defn write-manifest! [path compiled]
  (println "Writing" path)
  (let [manifest (map #(select-keys % [:name :hash :file]) compiled)]
    (spit path (prn-str manifest))))

(defn compile-styles! [assets]
  (let [compiled (compile-garden-assets assets)]
    (write-manifest! "resources/public/css/manifest.edn" compiled)
    (doseq [{:keys [file contents]} compiled]
      (println "Writing" file)
      (spit (str "resources/public/css/" file) contents))))

(comment
  (compile-garden-assets [{:name :test :styles [[:* {:color :blue}]]}])
  ;; => [{:name :test,
  ;;      :hash "9416bb727d77b551742ea0fda30a1ffa",
  ;;      :file "test.9416bb727d77b551742ea0fda30a1ffa.css",
  ;;      :contents "* {\n  color: blue;\n}"}]

  (write-manifest! "resources/public/css/manifest.edn"
                   [{:name :test :styles [[:* {:color :blue}]]}]))

(defn watch!
  "Watch namespaces for changes, compiling CSS when a stylesheet or
   any of its dependencies is updated."
  [build]
  (.start (Thread. (fn []
                     (compile-css-on-change build)))))