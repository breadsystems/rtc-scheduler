(ns rtc.register.core
  (:require
   [clojure.string :refer [join]]
   [reagent.core :as r]
   [reagent.dom :as dom]
   [rtc.users.passwords :as pass]))


(defonce app (r/atom {:email ""
                      :code ""
                      :user {:first_name ""
                             :last_name ""
                             :phone ""
                             :pass ""
                             :pass-confirmation ""}}
                     :errors {}))

(def user (r/cursor app [:user]))
(def errors (r/cursor app [:errors]))

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


(defn register!
  "The main event. Register the user and redirect on success."
  []
  (js/console.log (clj->js @user)))


(comment
  @app
  @user
  (field->errors :pass))


(defn errors-for [field]
  (if-let [messages (map :message (field->errors field))]
    [:div.error-message (join ", " messages)]
    [:<>]))


(defn registration []
  [:main
   [:h2 "Register"]
   [:div.stack
    [:div
     [:span [:strong "Email: "] (:email @app)]
     [:p.help "You can change your email once you finish setting up your account."]]
    [:div.flex-field
     [:label.field-label {:for "first_name"} "First Name"]
     [:div.field
      [:input {:type :text
               :id "first_name"
               :class (field->class :first_name)
               :on-change (emitter update-user-field! :first_name)
               :on-blur (emitter check-required! :first_name "Please enter your first name")
               :value (:first_name @user)}]
      [errors-for :first_name]]]
    [:div.flex-field
     [:label.field-label {:for "last_name"} "Last Name"]
     [:div.field
      [:input {:type :text
               :id "last_name"
               :class (field->class :last_name)
               :on-change (emitter update-user-field! :last_name)
               :on-blur (emitter check-required! :last_name "Please enter your last name")
               :value (:last_name @user)}]
      [errors-for :last_name]]]
    [:div.flex-field
     [:label.field-label {:for "phone"} "Phone"]
      ;; TODO phone input
     [:div.field
      [:input {:type :text
               :id "phone"
               :class (field->class :phone)
               :on-change (emitter update-user-field! :phone)
               :value (:phone @user)}]
      [errors-for :phone]]]
    [:div.flex-field
     [:label.field-label {:for "password"} "Password"]
     [:div.field
      [:input {:type :password
               :id "password"
               :class (field->class :pass)
               :on-change (emitter update-user-field! :pass)
               :on-blur #(check-password-fields!)
               :value (:pass @user)}]
      [errors-for :pass]]]
    [:div.flex-field
     [:label.field-label {:for "password-confirmation"} "Confirm Password"]
     [:div.field
      [:input {:type :password
               :id "password-confirmation"
               :class (field->class :pass-confirmation)
               :on-change (emitter update-user-field! :pass-confirmation)
               :on-blur #(check-password-fields!)
               :value (:pass-confirmation @user)}]
      [errors-for :pass-confirmation]]]
    [:div
     [:button.btn {:type :submit
                   :disabled (not @(r/track valid?))}
      "Register"]]]])


(defn ^:dev/after-load mount! []
  (dom/render [registration] (.getElementById js/document "rtc-registration")))

(defn init! []
  (let [email (.-content (.querySelector js/document "meta[name=email]"))
        code  (.-content (.querySelector js/document "meta[name=code]"))]
    (swap! app (fn [app]
                 (-> app
                     (assoc :email email)
                     (assoc :code code)))))
  (mount!))