;; Internationalization.
;; TODO The design of this still needs to be fleshed out.
(ns rtc.i18n.core)


(defn t
  "Translate a phrase identified by phrase-key into the given lang. If key
   does not exist at lang inside the given i18n map, returns nil."
  [{:keys [lang i18n]} phrase-key]
  (get-in i18n [lang phrase-key]))

(defn i18n->lang-options
  "Takes a map of i18n data keyed by lang (typically a keyword like :en-US)
   and returns a vector of maps of the form {:value :en-US :label \"English\"}"
  [i18n]
  (map (fn [{:keys [lang lang-name]}]
         {:value lang :label lang-name})
       (vals i18n)))

(defn supported-langs [i18n]
  (set (map name (keys i18n))))

(defn supported? [lang i18n]
  (contains? (supported-langs i18n) (name lang)))

(defn best-supported-lang [lang i18n]
  ;; I guess globalization is a necessary evil sometimes.
  (letfn [(globalize [lang]
            (apply str (take 2 (seq (name lang)))))]
    (cond
     (supported? lang i18n) (keyword lang)
     (supported? (globalize lang) i18n) (keyword (globalize lang))
     :else :en-US)))
