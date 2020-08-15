(ns rtc.i18n-core-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.i18n.core :as core :refer [t]]))


(deftest honors-current-lang
  (let [db {:lang :es
            :i18n {:es {:hello-world "Hola, Mundo!"}}}]
    (is (= "Hola, Mundo!"
           (t db :hello-world)))
    (is (nil? (t db :nonsense)))
    (is (nil? (t (assoc db :lang :fr) :hello-world)))))

(deftest test-i18n->lang-options
  (let [i18n {:en {:lang :en :lang-name "English"}
              :es {:lang :es :lang-name "Español"}}]
    (is (= [{:value :en :label "English"}
            {:value :es :label "Español"}]
           (core/i18n->lang-options i18n)))))

(deftest test-supported-langs
  (let [i18n {:en-US {} :es {}}]
    (is (= #{"en-US" "es"} (core/supported-langs i18n)))))

(deftest test-supported?
  (let [i18n {:en-US {} :es {}}]
    (is (core/supported? :en-US i18n))
    (is (core/supported? :es i18n))
    ;; Strings should work too
    (is (core/supported? "en-US" i18n))
    ;; We have no plans to support Esperanto, unfortunately.
    (is (not (core/supported? :eo i18n)))))

#_(deftest test-best-supported-lang
  (let [i18n {:en-US {} :es {}}]
    (is (= ))))