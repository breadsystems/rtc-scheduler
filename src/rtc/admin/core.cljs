(ns rtc.admin.core
  (:require
   [cljs.core.async :refer [<!]]
   [reagent.dom :as dom]
   [re-frame.core :as rf]
   [rtc.api.queries :as q :refer [->query-string]]
   [rtc.api :as api])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))


(rf/reg-event-db
 ::init-db
 (fn [_]
   {:users []
    :current-invite {:email ""}
    :greeting "Hello, world!"
    :my-invitations []}))

(rf/reg-sub ::current-invite (fn [db]
                               (:current-invite db)))

(rf/reg-sub ::my-invitations (fn [db]
                               (:my-invitations db)))

(comment
  @(rf/subscribe [::current-invite])
  @(rf/subscribe [::my-invitations]))

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
 ::init-admin
 (fn []
   (api/query! [:query [:invitations
                        {:redeemed false}
                        :email
                        :date_invited]]
               ::admin-data)))

(rf/reg-event-db
 ::admin-data
 (fn [db [_ {:keys [data errors]}]]
   (if (seq errors)
     ;; TODO
     (js/console.error errors)
     (assoc db :my-invitations (:invitations data)))))

(rf/reg-event-fx
 ::invite!
 (fn [db [_ {:keys [email]}]]
   (if (seq email)
     (api/query! [:mutate [:invite
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
  (rf/dispatch [::init-admin])
  (rf/dispatch [::update-invite-email "foo@example.com"])
  (rf/dispatch [::update-invite-email "foo2@example.com"])
  @(rf/subscribe [::my-invitations]))


(defn- invitation [{:keys [email code date_invited]}]
  [:div.invite
   [:strong.email email]
   [:em date_invited]
   (when code
     [:span.invite-url
      ;; oh god why
      (str "http://" js/window.location.host "/register?email=" email "&code=" code)])])


(defn dashboard []
  (let [current-invite @(rf/subscribe [::current-invite])
        my-invitations @(rf/subscribe [::my-invitations])]
    [:main
     [:h2 "Dashboard"]
     [:section
      [:h3 "Invite a comrade"]
      [:div.field
       [:label {:for "invite-email"} "Email"]
       [:input#invite-email {:value (:email current-invite)
                             :on-change #(rf/dispatch [::update-invite-email (.. % -target -value)])}]
       [:button {:on-click #(rf/dispatch [::invite! current-invite])
                 :disabled (empty? (:email current-invite))} "Invite!"]]]
     [:section
      [:h3 "Your invites"]
      (map
       (fn [invite]
         ^{:key (:email invite)}
         [invitation invite])
       my-invitations)]]))


(defn ^:dev/after-load mount! []
  (dom/render [dashboard] (.getElementById js/document "rtc-admin-app")))

(defn init! []
  (rf/dispatch-sync [::init-db])
  (rf/dispatch [::init-admin])
  (mount!))