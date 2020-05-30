(ns rtc.admin-core-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.admin.core :as admin]))


(deftest test-accessible-routes
  (is (admin/accessible-by? #{}
                            {:name :unrestricted-route
                             :restrict-to-roles #{}}))
  (is (admin/accessible-by? #{}
                            {:name :unrestricted-route}))
  (is (admin/accessible-by? #{:kin}
                            {:name :unrestricted-route}))
  (is (false? (admin/accessible-by? #{:kin}
                                    {:name :restricted-route
                                     :restrict-to-roles #{:doc}})))
  (is (admin/accessible-by? #{:kin :doc}
                            {:name :restricted-route
                             :restrict-to-roles #{:doc}})))