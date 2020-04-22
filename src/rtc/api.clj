(ns rtc.api
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [com.walmartlabs.lacinia :as l]
   [com.walmartlabs.lacinia.util :as util]
   [com.walmartlabs.lacinia.schema :as schema]
   [mount.core :as mount :refer [defstate]]))


(defn resolvers []
  {:query/careseeker_by_id (constantly {:name :bar})})

(defn load-schema []
  (-> (io/resource "graphql/schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolvers))
      schema/compile))


(defstate graphql-schema
  :start (load-schema))


(defn restart! []
  (mount/stop #'graphql-schema)
  (mount/start #'graphql-schema)
  nil)


(comment
  (restart!)

  (l/execute
   graphql-schema
   "{ careseeker_by_id(id: 1) { id name alias pronouns }}"
   nil nil))