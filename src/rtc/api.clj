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
   [rtc.api.queries :refer [->query-string]]
   [rtc.db :as db]
   [rtc.appointments.data :as appt]
   [rtc.users.core :as u]
   [rtc.users.handlers :refer [register-resolver]])
  (:import (clojure.lang IPersistentMap)))


(defn- id-resolver [f]
  (fn [_context {:keys [id]} _value]
    (f {:id id})))

(def ^:private default-uid
  (when (= "1" (System/getenv "DEV_DISABLE_AUTH")) 1))

(defn- req->uid [req]
  (get-in req [:session :identity :id] default-uid))

(defn resolvers []
  {:query/careseeker     (id-resolver db/get-careseeker)
   :query/provider       (id-resolver db/get-provider)
   :query/appointment    (id-resolver db/get-appointment)
   :query/availabilities (fn [_context args _value]
                           (db/get-availabilities (select-keys args [:start :end :state])))
   :query/appointments   (fn [_context args _value]
                           ;; TODO
                           [])
   :query/user           (id-resolver u/id->user)
   :query/users          (fn [_context args _value]
                           (u/filters->users (select-keys args [:count
                                                                :offset
                                                                :account_created_start
                                                                :account_created_end])))
   :query/invitations    (auth/admin-only-resolver
                          (fn [{:keys [request]} args _value]
                            (db/get-invitations (merge (select-keys args [:redeemed])
                                                       {:invited_by (req->uid request)}))))

   :mutation/invite      (auth/admin-only-resolver
                          (fn [{:keys [request]} {:keys [email]} _value]
                            (let [user-id (req->uid request)]
                              (u/invite! email user-id))))
   :mutation/register    register-resolver})

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

  ;; For privileged requests, eval this stuff first
  (def admin-1 {:is_admin true :id 1})
  (def admin-context {:request {:session {:identity admin-1}}})

  (q (->query-string [:query [:invitations
                              {:invited_by 1 :redeemed false}
                              :email
                              :code
                              :redeemed
                              :date_invited]])
     admin-context)

  (q (->query-string [:mutation [:invite {:email "new1234@example.com"} :email :code]])
     admin-context)

  (q (->query-string [:query [:careseeker {:id 1} :id :name :alias :pronouns]]))
  (q (->query-string [:query [:availabilities :id :start_time :end_time]]))

  (q (->query-string [:query [:availabilities
                              {:from "2020-08-11" :to "2020-08-13"}
                              :id
                              :start_time
                              :end_time]]))

  (q (->query-string [:query [:appointments :id]])))