(ns rtc.users.invites
  (:require
   ["moment" :as moment]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [rtc.rest.core :as rest]))


    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;     Subscriptions     ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub ::current-invite :current-invite)
(rf/reg-sub ::my-invitations :my-invitations)

(comment
  @(rf/subscribe [::current-invite])
  @(rf/subscribe [::my-invitations]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;        Events         ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(rf/reg-event-db
 :invites/load
 (fn [db [_ {:keys [data]}]]
   (assoc db :my-invitations (:invitations data))))

(rf/reg-event-db
 ::update-invite-email
 (fn [db [_ email]]
   (assoc-in db [:current-invite :email] email)))

(rf/reg-event-fx
 ::invite
 (fn [{:keys [db]} [_ {:keys [email]}]]
   (when (seq email)
     {::invite! {:csrf-token (:csrf-token db) :email email}})))

(rf/reg-fx
 ::invite!
 (fn [{:keys [email csrf-token]}]
   (rest/post! "/api/v1/admin/invite"
               {:transit-params {:email email}
                :headers {"x-csrf-token" csrf-token}}
               ::invite-generated)))

(rf/reg-event-db
 ::invite-generated
 (fn [db [_ {:keys [data]}]]
   (-> db
       (assoc :current-invite {:email ""})
       (update :my-invitations conj data))))

(comment
  (rf/dispatch [::update-invite-email "foo@example.com"])
  (rf/dispatch [::update-invite-email "foo2@example.com"])
  @(rf/subscribe [::my-invitations]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;      Components       ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- invitation [{:keys [email date_invited expired? redeemed url]}]
  [:div.invite
   [:div.invite__email
    [:span email]]
   [:div.invite__timing
    [:span (.fromNow (moment date_invited))]]
   ;; TODO copy code and/or send email
   (cond
     redeemed
     [:span "Redeemed"]
     expired?
     [:span "Expired"]
     :else
     [:span.invite-url url])])


(defn invites []
  (let [local-state (r/atom {:viewing-all? false})]
    (fn []
      (let [current-invite @(rf/subscribe [::current-invite])
            my-invitations @(rf/subscribe [::my-invitations])
            viewing-all? (:viewing-all? @local-state)
            display-count (if viewing-all? (count my-invitations) 3)]
        [:<>
         [:section
          [:h3 "Invite a comrade"]
          [:div.field
           [:form {:on-submit #(do (.preventDefault %)
                                 (rf/dispatch [::invite current-invite]))}
            [:label.field-label {:for "invite-email"} "Email"]
            [:div.field
             [:input#invite-email {:type :email
                                   :placeholder "comrade@riseup.net"
                                   :value (:email current-invite)
                                   :on-change #(rf/dispatch [::update-invite-email (.. % -target -value)])}]]
            [:button {:disabled (empty? (:email current-invite))} "Invite!"]]]]
         [:section
          [:h3 "Your invites"]
          [:p.help "To send an open invitation, copy and paste the URL in an email."]
          (map
           (fn [invite]
             ^{:key (:code invite)}
             [invitation invite])
           (take display-count my-invitations))
          [:button.secondary {:on-click
                              #(swap! local-state update :viewing-all? not)}
           (if viewing-all? "View fewer invites" "View all invites")]]]))))
