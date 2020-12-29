(ns rtc.admin.settings
  (:require
   [re-frame.core :as rf]))

(defn init-settings [db [_ {:keys [data]}]]
  (-> db
      (assoc :user-id (:id data))
      (assoc-in [:users (:id data)] data)))

(defn current-user [{:keys [users user-id]}]
  (get users user-id))

(defn update-setting [{:keys [user-id] :as db} [_ k v]]
  (assoc-in db [:users user-id k] v))


(rf/reg-event-db :settings/load init-settings)
(rf/reg-event-db ::update-setting update-setting)

(rf/reg-sub ::current-user current-user)

(comment
  @(rf/subscribe [::current-user]))

(defn- setting-field [{:keys [setting type label]}]
  (let [type (or type :text)
        settings @(rf/subscribe [::current-user])]
    [:div.flex-field
     [:label.field-label {:for (name setting)} label]
     [:div.field
      (cond
        (contains? #{:text :email :password} type)
        [:input {:type type
                 :id (name setting)
                 :on-change
                 #(rf/dispatch
                   [::update-setting setting (.. % -target -value)])
                 :value (get settings setting)}]
        (= :checkbox type)
        [:input {:type :checkbox
                 :id (name setting)
                 :on-change
                 #(rf/dispatch
                   [::update-setting setting (.. % -target -checked)])
                 :checked (boolean (get settings setting))}])]]))


(defn settings []
  [:div.stack
   [:div.stack.spacious
    [:h3 "Contact Info"]
    [setting-field {:setting :first_name
                    :label "First Name"}]
    [setting-field {:setting :last_name
                    :label "Last Name"}]
    [setting-field {:setting :email
                    :type :email
                    :label "Email"}]
    [setting-field {:setting :phone
                    :label "Phone"}]
    [setting-field {:setting :is_provider
                    :type :checkbox
                    :label "I am a provider"}]
    [:div
     [:button {:on-click #(prn 'UPDATE)}
      "Update Details"]]]

   [:div.stack.spacious
    [:h3 "Reset Password"]
    [setting-field {:setting :pass
                    :type :password
                    :label "New password"}]
    [setting-field {:setting :pass-confirmation
                    :type :password
                    :label "Confirm new password"}]
    [:button {:on-click #(prn 'PWD)}
     "Update Password"]]])