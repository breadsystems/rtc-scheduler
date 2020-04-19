(ns user
  (:require
   [clojure.test :as test]
   [mount.core :as mount]
   [rtc.env :refer [repl-server]]))


(defn restart []
  (mount/stop-except #'repl-server)
  (mount/start))


(comment
  (test/run-all-tests)
  (mount/stop-except #'repl-server)
  (mount/start)
  (restart))