;; Internationalization.
;; TODO The design of this still needs to be fleshed out.
(ns rtc.i18n.core)


(defn t
  "Translate a phrase into the given lang. Fetches from the db or something. I dunno."
  [phrase _lang]
  phrase)