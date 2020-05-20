(ns rtc.validators.core)


(defn validate-passwords [{:keys [password password-confirmation]}]
  (cond
    (empty? password)
    {:password [{:message "Please enter a password"}]
     :password-confirmation []}
    
    (< (count password) 8)
    {:password [{:message "Please choose a longer password"}]
     :password-confirmation []}
    
    (not= password password-confirmation)
    {:password []
     :password-confirmation [{:message "Passwords do not match"}]}
    
    :else
    true))