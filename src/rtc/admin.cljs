(ns rtc.admin)

(defonce !ws (atom nil))

(defn send! [action]
  (when-let [ws @!ws]
    (let [action (assoc action
                        :action/timestamp (js/Date.now))]
      (.send ws (pr-str action)))))

(defn ^:dev/after-load start []
  (reset! !ws (doto (js/WebSocket. "ws://localhost:4042/_ws")
                (.addEventListener "open" #(do
                                             (js/console.log "open!")
                                             (send! {:action/type :open})))
                (.addEventListener "close" (reset! !ws nil)))))

(defn init []
  (start))
