(ns rtc.layout
  (:require
   [buddy.auth :refer [authenticated?]]
   [hiccup.core :refer [html]]
   [hiccup.page :refer [doctype]]))


(defn error-page [opts]
  (let [{:keys [err req]} opts]
    {:status 400
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body
     (html
      (doctype :html5)
      [:html
       [:head
        [:title (str "ERROR: " err)]
        [:meta {:charset "utf-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:meta {:name "csrf-token" :content (:anti-forgery-token req)}]]
       [:body
        [:pre (str req)]]])}))


(defn page [opts]
  (let [{:keys [title head content footer-content req]} opts]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body
     (html
      (doctype :html5)
      [:html
       (vec (concat [:head
                     [:title (str title " | Radical Telehealth Collective")]
                     [:meta {:charset "utf-8"}]
                     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                     [:meta {:name "csrf-token" :content (:anti-forgery-token req)}]
                     [:link {:href "https://fonts.googleapis.com/css2?family=Libre+Franklin:wght@400;700&display=swap" :rel "stylesheet"}] 
                     [:link {:href "/css/screen.css" :rel "stylesheet"}]]
                    head))
       (conj [:body
              content]
             footer-content)])}))


(defn intake-page [opts]
  (page
   (merge
    opts
    {:head           []
     :content        [:div#rtc-intake-app]
     :footer-content [:div
                      [:script {:src "/js/shared.js" :type "text/javascript"}]
                      [:script {:src "/js/intake.js" :type "text/javascript"}]]})))


(defn admin-page [opts]
  (page
   (merge
    opts
    {:head           []
     :content        [:div#rtc-admin-app]
     :footer-content [:div
                      [:script {:src "/js/shared.js" :type "text/javascript"}]
                      [:script {:src "/js/admin.js" :type "text/javascript"}]]})))


(defn login-page [{:keys [error req]}]
  (let [{:keys [form-params query-string]} req
        {:keys [email password]} form-params]
    (page
     {:title "Login"
      :req req
      :content
      [:div.container.container--login
       [:header
        [:h1 "Radical Telehealth Collective"]
        [:h2 "Login"]]
       [:main
        [:form {:action (str "/login?" query-string) :method "POST"}
         (when error
           [:div.error
            [:p error]])
         [:input {:type :email
                  :name "email"
                  ;; TODO
                  :value (or email "")
                  :placeholder "me@example.com"}]
         [:input {:type :password
                  :name "password"
                  ;; TODO
                  :value (or password "")}]
         [:button {:type :submit} "Login"]]]]})))


(defn two-factor-page [{:keys [req error]}]
  (let [dest (get-in req [:query-params "next"])]
    (page
     {:title "Verify Token"
      :content
      [:div.container.container--2fa
       [:header
        [:h1 "Radical Telehealth Collective"]
        [:h2 "Login"]]
       [:form {:action (str "/login?next=" dest)
               :method "POST"}
        [:p "Please confirm the token shown in your authenticator app."]
        (when error
          [:div.error
           [:p error]])
        [:input {:type :text
                 :name "token"
                 :value ""
                 :placeholder "12 345 678"}]
        [:button {:type :submit} "Confirm"]]]})))