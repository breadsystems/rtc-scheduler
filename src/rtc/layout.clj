;; Basic page layouts for rendering server-side.
(ns rtc.layout
  (:require
   [buddy.auth :refer [authenticated?]]
   [hiccup.core :refer [html]]
   [hiccup.page :refer [doctype]]))


(defn error-page
  "For those uh-oh moments."
  [opts]
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


(defn page
  "Central page layout. If the server responds with HTML, this is what's
   getting called to produce the HTML document, unless there was a server error."
  [opts]
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


(defn intake-page
  "Render the intake Single-Page Application (SPA)."
  [opts]
  (page
   (merge
    opts
    {:head           [[:link {:rel "stylesheet" :href "/css/fullcalendar/main.min.css"}]
                      [:link {:rel "stylesheet" :href "/css/fullcalendar/timegrid.min.css"}]]
     :content        [:div#rtc-intake-app]
     :footer-content [:div
                      [:script {:src "/js/shared.js" :type "text/javascript"}]
                      [:script {:src "/js/intake.js" :type "text/javascript"}]]})))


(defn admin-page
  "Render the admin AKA Comrades Single-Page Application (SPA)."
  [opts]
  (page
   (merge
    opts
    {:head           []
     :content        [:div#rtc-admin-app]
     :footer-content [:div
                      [:script {:src "/js/shared.js" :type "text/javascript"}]
                      [:script {:src "/js/admin.js" :type "text/javascript"}]]})))


(defn login-page
  "Render the login page."
  [{:keys [error req]}]
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


(defn two-factor-page
  "Render the second step of the login form, 2-Factor Authentication (2FA)."
  [{:keys [req error]}]
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