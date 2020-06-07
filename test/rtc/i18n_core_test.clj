(ns rtc.i18n-core-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.i18n.core :refer [t]]))


(deftest honors-curren-lang
  (let [db {:lang :es
            :i18n {:es {:hello-world "Hola, Mundo!"}}}]
    (is (= "Hola, Mundo!"
           (t db :hello-world)))
    (is (nil? (t db :nonsense)))
    (is (nil? (t (assoc db :lang :fr) :hello-world)))))