(ns rtc.reset-pass.core
  (:require
   [clojure.string :refer [join]]
   [reagent.core :as r]
   [reagent.dom :as dom]
   [rtc.rest.core :as rest]
   [rtc.users.passwords :as pass]))


(defonce app (r/atom {:user {:code ""
                             :email ""
                             :pass ""
                             :pass-confirmation ""}}
                     :errors {}))

(def user (r/cursor app [:user]))
(def errors (r/cursor app [:errors]))
(def global-errors (r/cursor app [:errors :global]))

(defn valid? []
  (every? (comp empty? val) @errors))

(defn update-user-field! [field value]
  (swap! app assoc-in [:user field] value))

(defn set-field-errors! [errors]
  (swap! app update-in [:errors] (fn [current]
                                   (merge current errors))))

(defn field->errors [field]
  (get @errors field []))

(defn field->class [field]
  (when (seq (field->errors field)) "has-errors"))

(defn check-required! [field message value]
  (let [field-errors (if (seq value) [] [{:message message}])]
    (swap! app assoc-in [:errors field] field-errors)))

(defn check-password-fields! []
  (let [validation (pass/validate-passwords @user)]
    (if (true? validation)
      (set-field-errors! {:pass [] :pass-confirmation []})
      (set-field-errors! validation))))

(defn emitter
  "Returns a function that closes around f and any additional args,
   and calls f with the event target's value"
  [f & args]
  (fn [event]
    ((apply partial f args) (.. event -target -value))))

(defn on-reset-success [{:keys [data errors]}]
  (if (seq errors)
    (set-field-errors! {:global errors})
    (set! js/document.location (:redirect-to data))))

(defn reset-pass!
  "The main event. Reset the password and redirect on success."
  []
  (rest/post! "/api/v1/reset-pass"
              {:transit-params @user}
              on-reset-success
              (fn [{:keys [errors]}]
                (set-field-errors! {:global errors}))))


(comment
  @app
  @user
  @errors

  ;; Populate all fields
  (do
    (update-user-field! :pass "password")
    (update-user-field! :pass-confirmation "password"))

  ;; Test with an invalid invite
  (swap! app assoc-in [:user :email] "")

  (reset-pass!)

  (field->errors :pass))


(defn errors-for [field]
  (if-let [messages (map :message (field->errors field))]
    [:div.error-message (join ", " messages)]
    [:<>]))


(defn registration []
  (let [update-pass! (emitter update-user-field! :pass)
        update-confirmation! (emitter update-user-field! :pass-confirmation)]
    [:main
     [:h2 "Reset Password"]
     [:form.stack {:on-submit (fn [e]
                                (.preventDefault e)
                                (reset-pass!))}
      (when (seq @global-errors)
        (doall (for [err @global-errors]
                 ^{:key (:message err)}
                 [:p.error-message (:message err)])))
      [:div.flex-field
       [:label.field-label {:for "password"} "New Password"]
       [:div.field
        [:input {:type :password
                 :id "password"
                 :class (field->class :pass)
                 :on-change #(do
                               (update-pass! %)
                               (check-password-fields!))
                 :value (:pass @user)}]
        [errors-for :pass]]]
      [:div.flex-field
       [:label.field-label {:for "password-confirmation"} "Confirm Password"]
       [:div.field
        [:input {:type :password
                 :id "password-confirmation"
                 :class (field->class :pass-confirmation)
                 :on-change #(do
                               (update-confirmation! %)
                               (check-password-fields!))
                 :value (:pass-confirmation @user)}]
        [errors-for :pass-confirmation]]]
      [:div
       [:button.btn {:type :submit
                     :disabled (not @(r/track valid?))}
        "Reset!"]]]]))


(defn ^:dev/after-load mount! []
  (dom/render [registration] (.getElementById js/document "rtc-registration")))

(defn ^:export init! []
  (let [email (.-content (.querySelector js/document "meta[name=email]"))
        code  (.-content (.querySelector js/document "meta[name=code]"))]
    (swap! app (fn [app]
                 (-> app
                     (assoc-in [:user :email] email)
                     (assoc-in [:user :code] code)))))
  (mount!))
