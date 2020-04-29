(ns rtc.layout)


(defn page [opts]
  (let [{:keys [content]} opts]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body content}))