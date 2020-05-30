(ns rtc.register.core
  (:require
   [clojure.string :refer [join]]
   [reagent.core :as r]
   [reagent.dom :as dom]
   [rtc.users.passwords :as pass]))


(defonce app (r/atom {:email ""
                      :code ""
                      :user {:first-name ""
                             :last-name ""
                             :phone ""
                             :password ""
                             :password-confirmation ""}}
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
  (when (seq (field->errors field)) "error"))
    

(defn check-required! [field message value]
  (let [field-errors (if (seq value) [] [{:message message}])]
    (swap! app assoc-in [:errors field] field-errors)))

(defn check-password-fields! []
  (let [validation (pass/validate-passwords @user)]
    (if (true? validation)
      (set-field-errors! {:password [] :password-confirmation []})
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
  (field->errors :password))


(defn errors-for [field]
  (if-let [messages (map :message (field->errors field))]
    [:div (join ", " messages)]
    [:<>]))


(defn registration []
  [:main
   [:h2 "Register"]
   [:div
    [:div
     [:b "Email: "] (:email @app)
     [:p "You can change your email once you finish setting up your account."]]
    [:div.field {:class (field->class :first-name)}
     [:label {:for "first-name"} "First Name"]
     [:input {:id "first-name"
              :on-change (emitter update-user-field! :first-name)
              :on-blur (emitter check-required! :first-name "Please enter your first name")
              :value (:first-name @user)}]
     [errors-for :first-name]]
    [:div.field {:class (field->class :last-name)}
     [:label {:for "last-name"} "Last Name"]
     [:input {:id "last-name"
              :on-change (emitter update-user-field! :last-name)
              :on-blur (emitter check-required! :last-name "Please enter your last name")
              :value (:last-name @user)}]
     [errors-for :last-name]]
    [:div.field
     [:label {:for "phone"} "Phone"]
      ;; TODO phone input
     [:input {:id "phone"
              :on-change (emitter update-user-field! :phone)
              :value (:phone @user)}]
     [errors-for :phone]]
    [:div.field {:class (field->class :password)}
     [:label {:for "password"} "Password"]
     [:input {:type :password
              :id "password"
              :on-change (emitter update-user-field! :password)
              :on-blur #(check-password-fields!)
              :value (:password @user)}]
     [errors-for :password]]
    [:div.field {:class (field->class :password-confirmation)}
     [:label {:for "password-confirmation"} "Confirm Password"]
     [:input {:type :password
              :id "password-confirmation"
              :on-change (emitter update-user-field! :password-confirmation)
              :on-blur #(check-password-fields!)
              :value (:password-confirmation @user)}]
     [errors-for :password-confirmation]]
    [:div
     [:button.btn {:type :submit
                   :disabled (not @(r/track valid?))
                   :on-click #(register!)}
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