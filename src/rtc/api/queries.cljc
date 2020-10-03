;; TODO retire this ns
(ns rtc.api.queries
  (:require
   [clojure.string :as string]
   #?(:cljs
      ["moment" :as moment])))


(defprotocol QueryNode
  (->query-string [node]))

(defprotocol QueryArgValue
  (->query-arg-value [v]))

(extend-protocol QueryNode
  #?(:clj java.lang.String :cljs string)
  (->query-string [s] s)

  #?(:clj clojure.lang.Keyword :cljs cljs.core.Keyword)
  (->query-string [k] (name k))

  #?(:clj clojure.lang.IPersistentMap :cljs cljs.core.PersistentArrayMap)
  (->query-string [m]
    (str "(" (string/join ", " (map ->query-string m)) ")"))

  #?(:cljs cljs.core.PersistentHashMap)
  #?(:cljs (->query-string [m]
             (str "(" (string/join ", " (map ->query-string m)) ")")))

  #?(:clj clojure.lang.MapEntry :cljs cljs.core.MapEntry)
  (->query-string [entry]
    (str (->query-string (key entry)) ": " (->query-arg-value (val entry))))

  #?(:clj clojure.lang.PersistentVector :cljs cljs.core.PersistentVector)
  (->query-string [[handle & rest]]
    (str (->query-string handle)
         (if (map? (first rest))
           (str (->query-string (first rest))
                " { "
                (string/join " " (map ->query-string (next rest)))
                " }")
           (str " { "
                (string/join " " (map ->query-string rest))
                " }")))))


(defn- double-quotes [s]
  (str "\"" s "\""))

(extend-protocol QueryArgValue
  #?(:clj java.lang.String :cljs string)
  (->query-arg-value [s] (double-quotes s))

  #?(:clj java.lang.Long :cljs number)
  (->query-arg-value [n] n)

  #?(:clj clojure.lang.Keyword :cljs cljs.core.Keyword)
  (->query-arg-value [k] (double-quotes (name k))) 
  
  #?(:clj java.lang.Boolean :cljs boolean)
  (->query-arg-value [b] (str b)))

#?(:cljs
   (extend-protocol QueryArgValue
     js/Date
     (->query-arg-value [dt] (.format (moment dt)))))


(comment
  (->query-string [:mutation
                   [:invite {:email "a@b.com"}
                    :email
                    :code]])

  (->query-string {:a :b}))