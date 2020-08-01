(ns rtc.api.core
  (:require
   [cljs.core.async :refer [<!]]
   [cljs-http.client :as http]
   [re-frame.core :as rf]
   [rtc.api.queries :refer [->query-string]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))


(defn query->then [query f]
  (go (let [req {:body (->query-string query)
                 :headers {"Content-Type" "application/graphql"}}
            response (<! (http/post "/api/graphql" req))]
        (f response))))

(defn query! [query event]
  (query->then
   query
   (fn [response]
     (rf/dispatch [event (apply hash-map (:body response))]))))

(defn debug-query! [query]
  (query->then
   query
   (fn [response]
     (let [body (apply hash-map (:body response))]
       (js/console.log (clj->js body))
       body))))


(comment
  (debug-query! [:query
                 [:appointment {:id }]])
  (debug-query! [:mutation
                 [:invite {:email "abc@example.email"}
                  :code :email]]))