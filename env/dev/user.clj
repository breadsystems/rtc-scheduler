(ns user
  (:require
   [clojure.test :as test]
   [mount.core :as mount]))


(defn restart []
  (mount/stop)
  (mount/start))


(comment
  (test/run-all-tests)
  (mount/stop)
  (mount/start)
  (restart))