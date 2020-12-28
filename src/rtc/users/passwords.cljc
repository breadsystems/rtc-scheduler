(ns rtc.users.passwords)


(defn validate-passwords [{:keys [pass pass-confirmation]}]
  (cond
    (empty? pass)
    {:pass [{:message "Please enter a password"}]
     :pass-confirmation []}
    
    (< (count pass) 8)
    {:pass [{:message "Please choose a longer password"}]
     :pass-confirmation []}
    
    (not= pass pass-confirmation)
    {:pass []
     :pass-confirmation [{:message "Passwords do not match"}]}
    
    :else
    true))