(ns rtc.api.core
  (:require
   [cljs.core.async :refer [<!]]
   [cljs-http.client :as http]
   [re-frame.core :as rf]
   [rtc.api.queries :refer [->query-string]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))


(defn query! [query event]
  (go (let [req {:body (->query-string query)
                 :headers {"Content-Type" "application/graphql"}}
            response (<! (http/post "/api/graphql" req))]
        (rf/dispatch [event (apply hash-map (:body response))]))))
