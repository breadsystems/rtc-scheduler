(ns user
  (:require
   [mount.core :as mount]
   [rtc.env :refer [repl-server]]))


(defn restart []
  (mount/stop-except #'repl-server)
  (mount/start))


(comment
  (mount/stop-except #'repl-server)
  (mount/start)
  (restart))