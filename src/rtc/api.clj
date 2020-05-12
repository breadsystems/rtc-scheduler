(ns rtc.api
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.walk :as walk]
   [com.walmartlabs.lacinia :as l]
   [com.walmartlabs.lacinia.util :as util]
   [com.walmartlabs.lacinia.schema :as schema]
   [mount.core :as mount :refer [defstate]]
   [rtc.auth :as auth]
   [rtc.db :as db]
   [rtc.appointments.data :as appt]
   [rtc.users.core :as u])
  (:import (clojure.lang IPersistentMap)))


(defn- id-resolver [f]
  (fn [_context {:keys [id]} _value]
    (f {:id id})))

(defn resolvers []
  {:query/careseeker     (id-resolver db/get-careseeker)
   :query/provider       (id-resolver db/get-provider)
   :query/appointment    (id-resolver db/get-appointment)
   :query/availabilities (fn [_context args _value]
                           (db/get-availabilities (select-keys args [:start :end :state])))
   :query/appointments   (fn [_context args _value]
                           ;; TODO
                           [])

   :mutation/invite      (auth/admin-only-resolver
                          (fn [{:keys [request]} {:keys [email]} _value]
                            (let [user-id (get-in request [:session :identity :id])]
                              (u/invite! email user-id))))})

(defn load-schema []
  (-> (io/resource "graphql/schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolvers))
      schema/compile))

(defn simplify
  "Converts all ordered maps nested within the map into standard hash maps, and
   sequences into vectors, which makes for easier constants in the tests, and eliminates ordering problems."
  [m]
  (walk/postwalk
   (fn [node]
     (cond
       (instance? IPersistentMap node) (into {} node)
       (seq? node)                     (vec node)
       :else                           node))
   m))


(defstate graphql-schema
  :start (load-schema))


(defn q
  ([query-string]
   (q query-string {}))
  ([query-string context]
   (-> (l/execute graphql-schema query-string nil context)
       simplify)))

(defn restart! []
  (mount/stop #'graphql-schema)
  (mount/start #'graphql-schema)
  nil)


(comment
  (restart!)

  (get-in (q "bogus query") [:errors 0 :message])

  (q "mutation { invite(email: \"new1234@example.com\") { email code }}"
     {:request {:session {:identity {:is_admin true :id 1}}}})

  (q "{ careseeker (id: 1) { id name alias pronouns }}")
  (q "{ availabilities { id start_time end_time }}")

  (q (str "{ availabilities"
          "(from: \"2020-08-11T00:00:00.000-08:00\","
          "to: \"2020-08-13T00:00:00.000-08:00\")"
          "{ id }}"))
  
  (q (str "{ appointments { id }}")))