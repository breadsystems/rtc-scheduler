;; Macro namespace for Internationalization (i18n) data
;; We need a macro for this because at runtime we do not have access to
;; the filesystem. So instead, we want to parse .edn files at compile time,
;; expanding the contents into per-language maps and initializing our
;; client-side database with the language data.
;;
;; READ MORE:
;;
;; * ClojureScript Macros by Thomas Heller (author of shadow-cljs):
;;   https://code.thheller.com/blog/shadow-cljs/2019/10/12/clojurescript-macros.html
(ns rtc.i18n.data
  (:require-macros
   [rtc.i18n.data :as macro]))