(ns rtc.admin.schedule)


(defn schedule [_]
  {:success true
   :data {:appointments   []
          :availabilities []
          :users          []}})