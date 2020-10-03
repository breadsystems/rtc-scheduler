;; TODO retire this ns
(ns rtc.api-queries-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.api.queries :as q :refer [->query-string]]))


(deftest test-->query-string
  (is (= "abc" (->query-string "abc")))
  (is (= "abc" (->query-string :abc)))
  (is (= "(email: \"abc@example.email\")"
         (->query-string {:email "abc@example.email"})))
  (is (= "(email: \"abc@example.email\", id: 1234)"
         (->query-string {:email "abc@example.email" :id 1234})))
  (is (= "(key: \"value\")"
         (->query-string {:key :value})))
  (is (= "appointments(from: \"2020-01-01\", to: \"2020-02-29\") { code email }"
         (->query-string [:appointments {:from "2020-01-01" :to "2020-02-29"}
                          :code
                          :email])))
  (is (= "invitations(redeemed: false) { code email }"
         (->query-string [:invitations {:redeemed false}
                          :code
                          :email])))
  (is (= "appointments { code email }"
         (->query-string [:appointments
                          :code
                          :email])))
  (is (= "mutation { invite(email: \"abc@example.email\", id: 1234) { code email } }"
         (->query-string [:mutation
                          [:invite {:email "abc@example.email" :id 1234}
                           :code
                           :email]]))))