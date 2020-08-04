(ns rtc.appointments.windows
  (:require
   [clojure.set :refer [union]]
   [clojure.spec.alpha :as spec]
   [rtc.appointments.internal.time :as t]))



;; (defn- apply-op [id->toggle [id op]]
;;   (update id->toggle id + op))

;; (defn- apply-ops [id->toggle [[_ id op] & ops]]
;;   (if op
;;     (update (apply-ops id->toggle ops) id #(+ (or % 0) op))
;;     id->toggle))

;; (defn- toggles->pids [toggles]
;;   (let [positives (filter (fn [[_ v]] (= 1 v)) toggles)]
;;     (into [] (map key positives))))

;; (defn- id->appt-count [id appts]
;;   (count (filter #(= id (:id %)) appts)))

;; (defn- curtail [earliest latest {:keys [start end] :as e}]
;;   (assoc e
;;          :start (max earliest start)
;;          :end (max latest end)))

;; (defn- avail->ops [from to w factor {:keys [start end id]}]
;;   (if (or (> start to) ;; 
;;           (> from end)
;;           (> w (- from start))
;;           (> w (- to end)))
;;     []
;;     (let [start (- (max start from) (mod (Math/abs (max start from)) w))
;;           end   (+ (min end to) (mod (Math/abs (min end to)) w))]
;;       [[start id factor] [end id (* factor -1)]])))

;; (defn- fold-ops [t->ops [start id op]]
;;   (update t->ops start conj [start id op]))


(defn ->windows [avails appts from to w]
  {:pre [(> to from)
         (>= (- to from) w)
         (pos-int? w)
         ;; TODO validate avails
         ;; TODO validate appts
         ]}

  (let []
    []))

(comment
  (def date->pid->toggle {0   {1 0, 2 0, 3 0}
                          50  {1 0, 2 0, 3 0}
                          100 {1 0, 2 0, 3 0}})

  (update date->pid->toggle 50 (fn [p->t]
                                 (apply-ops p->t [[3 1]])))


  (id->appt-count 1 [{:id 1} {:id 2} {:id 1}])

  (reduce concat [] (concat [[[:op 1] [:op 2]] [[:op 3] [:op 4]]] [[[:op 5]] [[:op 6]]]))

  ;;  
  )