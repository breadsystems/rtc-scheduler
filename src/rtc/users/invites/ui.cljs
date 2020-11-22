;; TODO move to rtc.users.invites
(ns rtc.users.invites.ui
  (:require
   [re-frame.core :as rf]))


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
 ::update-invite-email
 (fn [db [_ email]]
   (assoc-in db [:current-invite :email] email)))

(rf/reg-event-db
 ::my-invitations
 (fn [db [_ payload]]
   (assoc db :my-invitations payload)))

(rf/reg-event-db
 ::invite-generated
 (fn [db [_ {:keys [data error]}]]
   (js/console.log data error)
   (update db :my-invitations conj data)))


(rf/reg-event-fx
 ::invite!
 (fn [db [_ {:keys [email]}]]
   (if (seq email)
     ;; TODO rest/post!
     (prn email)
     #_(api/query! [:mutate [:invite
                             {:email email}
                             :email
                             :date_invited
                             :code]]
                   ::on-invite-response)
     (js/console.error "no email?"))))

(rf/reg-event-db
 ::on-invite-response
 (fn [db [_ {:keys [data errors]}]]
   (-> db
       (assoc :current-invite {:email ""})
       (update :my-invitations conj (merge (:current-invite db)
                                           {:date_invited "Just now"
                                            :code "asdfqwerty_x234"})))))

(comment
  (rf/dispatch [::update-invite-email "foo@example.com"])
  (rf/dispatch [::update-invite-email "foo2@example.com"])
  @(rf/subscribe [::my-invitations]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                       ;;
  ;;      Components       ;;
 ;;                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- invitation [{:keys [email code date_invited]}]
  [:div.invite
   [:strong.email email]
   [:em date_invited]
   (when code
     [:span.invite-url
      ;; oh god why
      (str "http://" js/window.location.host "/register?email=" email "&code=" code)])])


(defn invites []
  (let [current-invite @(rf/subscribe [::current-invite])
        my-invitations @(rf/subscribe [::my-invitations])]
    [:<>
     [:section
      [:h3 "Invite a comrade"]
      [:div.field
       [:form {:on-submit #(do (.preventDefault %)
                             (rf/dispatch [::invite! current-invite]))}
        [:label.field-label {:for "invite-email"} "Email"]
        [:div.field
         [:input#invite-email {:type :email
                               :placeholder "comrade@riseup.net"
                               :value (:email current-invite)
                               :on-change #(rf/dispatch [::update-invite-email (.. % -target -value)])}]]
        [:button {:disabled (empty? (:email current-invite))} "Invite!"]]]]
     [:section
      [:h3 "Your invites"]
      (map
       (fn [invite]
         ^{:key (:email invite)}
         [invitation invite])
       my-invitations)]]))